package org.chesmapper.view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.ClusteringImpl;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.gui.MainPanel.JmolPanel;
import org.jmol.export.dialog.Dialog;
import org.jmol.viewer.Viewer;
import org.mg.javalib.gui.ResolutionPanel;
import org.mg.javalib.util.DoubleArraySummary;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.Vector3fUtil;

public class View
{
	private Viewer viewer;
	GUIControler guiControler;
	public static View instance;
	ViewControler viewControler;
	private ClusteringImpl clustering;

	public boolean antialiasOn = false;

	private static Zoomable zoomedTo;

	public static enum AnimationSpeed
	{
		SLOW, FAST
	}

	private View(Viewer viewer, GUIControler guiControler, ViewControler viewControler, final ClusteringImpl clustering)
	{
		this.viewer = viewer;
		this.guiControler = guiControler;
		this.viewControler = viewControler;
		this.clustering = clustering;

		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_REMOVED)
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_MODIFIED)
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_CLEAR))
				{
					medianDiameter = null;
					for (Compound m : spheresForCompound)
						if (!clustering.getCompounds(true).contains(m))
							hideSphere(m);
				}
			}
		});

		viewer.script("set disablePopupMenu on");
		viewer.script("set minPixelSelRadius 30");

		setAntialiasOn(viewControler.isAntialiasEnabled());
		hideHydrogens(viewControler.isHideHydrogens());
	}

	public static View init(JmolPanel jmolPanel, GUIControler guiControler, ViewControler viewControler,
			ClusteringImpl clustering)
	{
		instance = new View((Viewer) jmolPanel.getViewer(), guiControler, viewControler, clustering);
		return instance;
	}

	public synchronized void setAntialiasOn(boolean antialias)
	{
		this.antialiasOn = antialias;
		if (antialias)
			viewer.script("set antialiasDisplay ON");
		else
			viewer.script("set antialiasDisplay OFF");
	}

	public boolean isAntialiasOn()
	{
		return antialiasOn;
	}

	public synchronized void setSpinEnabled(boolean spinEnabled, int speed)
	{
		if (spinEnabled)
		{
			System.out.println("spinning at " + speed);
			viewer.evalString("set spinx 0");
			viewer.evalString("set spiny " + speed);
			viewer.evalString("set spinz 0");
			viewer.evalString("spin on");
		}
		else
		{
			viewer.evalString("spin off");
		}
	}

	public synchronized void centerAt(Zoomable zoomable)
	{
		zoomTo(zoomable, null, null, true);
	}

	public synchronized void zoomTo(Zoomable zoomable, AnimationSpeed speed)
	{
		zoomTo(zoomable, speed, null);
	}

	private void checkNoAWT()
	{
		SwingUtil.checkNoAWTEventThread();
		if (guiControler.isVisible() && !guiControler.isBlocked())
			throw new IllegalStateException("animation running, gui should be blocked");
	}

	public synchronized void zoomTo(final Zoomable zoomable, final AnimationSpeed speed, Boolean superimposed)
	{
		zoomTo(zoomable, speed, superimposed, false);
	}

	private synchronized void zoomTo(final Zoomable zoomable, final AnimationSpeed speed, Boolean superimposed,
			boolean onlyCentering)
	{
		if (superimposed == null)
			superimposed = zoomable.isSuperimposed();
		final float diameter = Math.max(5.0f, zoomable.getDiameter(superimposed));
		final Vector3f center = zoomable.getCenter(superimposed);

		//		Settings.LOGGER.warn("zoom to " + zoomable);
		//		Settings.LOGGER.warn("Superimposed " + superimposed);
		//		Settings.LOGGER.warn("Center       " + center);
		//		Settings.LOGGER.warn("Diameter     " + diameter);
		//		Settings.LOGGER.warn("Rot radius   " + viewer.getRotationRadius());

		// old way of zooming
		//		int zoom = (int) ((1200 / (10 / viewer.getRotationRadius())) / diameter);
		//		Settings.LOGGER.warn("zoom          " + zoom);
		//		zoom = (int) Math.max(5, zoom);
		//		Settings.LOGGER.warn("zoom A        " + zoom);

		// much better, adjust the rotation radius instead
		viewer.setRotationRadius(diameter / 10.0f, true);

		//Settings.LOGGER.warn("Rot radius A " + viewer.getRotationRadius());
		int zoom = 10;

		if (isAnimated() && !onlyCentering)
		{
			checkNoAWT();
			final int finalZoom = zoom;
			boolean setAntialiasBackOn = false;
			if (viewControler.isAntialiasEnabled() && antialiasOn)
			{
				setAntialiasBackOn = true;
				setAntialiasOn(false);
			}
			String cmd = "zoomto " + (speed == AnimationSpeed.SLOW ? 0.66 : 0.33) + " " + Vector3fUtil.toString(center)
					+ " " + finalZoom;
			//			Settings.LOGGER.warn("XX zoom> " + cmd);
			viewer.scriptWait(cmd);

			System.out.println("resetting navigtion point");
			//viewer.setBooleanProperty("windowCentered", false);
			viewer.setBooleanProperty("windowCentered", true);

			if (setAntialiasBackOn)
				setAntialiasOn(true);
			zoomedTo = zoomable;
		}
		else
		{
			if (!onlyCentering)
			{
				String cmd = "zoomto 0 " + Vector3fUtil.toString(center) + " " + zoom;
				viewer.scriptWait(cmd);
			}
			else
			{
				// cannot use the setNewRotationCenter method
				// problem is that it calculates a new setRotationRadius and uses the old zoom factor 10
				// viewer.setNewRotationCenter(new Point3f(zoomable.getCenter(superimposed)));
				viewer.setRotationCenterWithoutRadius(new Point3f(zoomable.getCenter(superimposed)));
			}
			zoomedTo = zoomable;
		}

	}

	public Zoomable getZoomTarget()
	{
		return zoomedTo;
	}

	public synchronized static String convertColor(Color col)
	{
		return "[" + col.getRed() + ", " + col.getGreen() + ", " + col.getBlue() + "]";
	}

	public synchronized static String convertPos(Point3f p)
	{
		return "{" + p.x + " " + p.y + ", " + p.z + "}";
	}

	public synchronized void setBackground(Color col)
	{
		viewer.script("background " + convertColor(col));
	}

	public synchronized int findNearestAtomIndex(int x, int y)
	{
		return viewer.findNearestAtomIndexFixed(x, y);
	}

	public synchronized int getAtomCompoundIndex(int atomIndex)
	{
		return viewer.getAtomModelIndex(atomIndex);
	}

	public synchronized void clearSelection()
	{
		viewer.clearSelection();
	}

	public synchronized void select(BitSet bitSet)
	{
		//		Settings.LOGGER.warn("XX> selecting bitset with " + bitSet.cardinality() + " atoms, bitset: " + bitSet);
		viewer.select(bitSet, false, null, false);
		//		Settings.LOGGER.warn("XX> " + viewer.getAtomSetCenter(bitSet));
	}

	private void evalScript(String script)
	{
		if (script.matches("(?i).*hide.*") || script.matches("(?i).*subset.*") || script.matches("(?i).*display.*"))
			throw new Error("use wrap methods");
	}

	public synchronized void scriptWait(String script)
	{
		evalScript(script);
		//		Settings.LOGGER.warn("XX wait> " + script);
		viewer.scriptWait(script);
	}

	public synchronized void selectAll()
	{
		viewer.scriptWait("select not hidden");
	}

	public synchronized void hide(BitSet bs)
	{
		//		Settings.LOGGER.warn("XX> hide bitset with " + bs.cardinality() + " atoms, bitset: " + bs);
		viewer.select(bs, false, null, false);
		hideSelected();
	}

	public synchronized void hideSelected()
	{
		//		Settings.LOGGER.warn("XX> select selected OR hidden; hide selected");
		viewer.scriptWait("select selected OR hidden; hide selected");
	}

	public synchronized void display(BitSet bs)
	{
		//		Settings.LOGGER.warn("XX> display bitset with " + bs.cardinality() + " atoms, bitset: " + bs);
		viewer.select(bs, false, null, false);
		viewer.scriptWait("select (not hidden) OR selected; select not selected; hide selected");
	}

	public synchronized BitSet getCompoundBitSet(int modelIndex)
	{
		return viewer.getModelUndeletedAtomsBitSet(modelIndex);
	}

	public synchronized String getCompoundNumberDotted(int i)
	{
		return viewer.getModelNumberDotted(i);
	}

	public synchronized Point3f getAtomSetCenter(BitSet bitSet)
	{
		return viewer.getAtomSetCenter(bitSet);
	}

	HashSet<Compound> spheresForCompound = new HashSet<Compound>();
	public double sphereSize = 0.5;
	public double sphereTranslucency = 0.5;

	public synchronized void hideSphere(Compound m)
	{
		if (spheresForCompound.contains(m))
		{
			String id = "sphere" + m.getJmolIndex();
			scriptWait("ellipsoid ID " + id + " color translucent 1.0");
			scriptWait("ellipsoid ID " + id + "_2 color translucent 1.0");
		}
	}

	public Float medianDiameter = null;

	private synchronized double medianDiameter()
	{
		if (medianDiameter == null)
		{
			List<Float> d = new ArrayList<Float>();
			for (Compound m : clustering.getCompounds(true))
				d.add(m.getDiameter());
			medianDiameter = (float) DoubleArraySummary.create(d).getMedian();
		}
		return medianDiameter;
	}

	private synchronized void updateSpherePosition(Compound m)
	{
		if (spheresForCompound.contains(m))
		{
			String id = "sphere" + m.getJmolIndex();

			//			BoxInfo info = viewer.getBoxInfo(m.getBitSet(), 1.0F);
			//				Point3f center = info.getBoundBoxCenter();
			//				Vector3f corner = info.getBoundBoxCornerVector();
			//				scriptWait("ellipsoid ID " + id + " AXES {" + Math.max(corner.x * sphereSize, 1.0) + " 0 0} {0 "
			//						+ Math.max(corner.y * sphereSize, 1.0) + " 0} {0 0 " + Math.max(corner.z * sphereSize, 1.0)
			//						+ "}");
			//				scriptWait("ellipsoid ID " + id + " center " + convertPos(center));

			double mSize = medianDiameter() + ((m.getDiameter() - medianDiameter()) * 0.25);
			mSize = Math.min(8.0, mSize);
			double size = Math.max(1.0, mSize * 0.5 * (0.1 + 0.9 * sphereSize));

			scriptWait("ellipsoid ID " + id + " AXES {" + size + " 0 0} {0 " + size + " 0} {0 0 " + size + "}");
			scriptWait("ellipsoid ID " + id + " center " + convertPos(getAtomSetCenter(m.getBitSet())));

			scriptWait("ellipsoid ID " + id + "_2 AXES {" + size * 1.5 + " 0 0} {0 " + size * 1.5 + " 0} {0 0 " + size
					* 0.55 + "}");
			scriptWait("ellipsoid ID " + id + "_2 center " + convertPos(getAtomSetCenter(m.getBitSet())));

			//			Point3f c = getAtomSetCenter(m.getBitSet());
			//			Point3f p1 = new Point3f(c.x, c.y - (float) size * 1.5f, c.z + (float) size * 1.5f);
			//			Point3f p2 = new Point3f(c.x, c.y + (float) size * 1.5f, c.z - (float) size * 1.5f);
			//			Point3f p3 = new Point3f(c.x, c.y, c.z);
			//			scriptWait("draw ID " + id + "P plane " + convertPos(p1) + " " + convertPos(p2) + " " + convertPos(p3));
		}
	}

	public synchronized void showSphere(Compound m, boolean showLastHighlightColor, boolean updateSizeAndPos)
	{
		String id = "sphere" + m.getJmolIndex();
		if (!spheresForCompound.contains(m) || updateSizeAndPos)
		{
			spheresForCompound.add(m);
			updateSpherePosition(m);
		}

		double trans = 0.0 + 0.8 * sphereTranslucency;
		switch (m.getTranslucency())
		{
			case ModerateWeak:
				trans = 0.2 + 0.65 * sphereTranslucency;
				break;
			case ModerateStrong:
				trans = 0.4 + 0.5 * sphereTranslucency;
				break;
			case Strong:
				trans = 0.6 + 0.35 * sphereTranslucency;
				break;
			case None:
				//do nothing
		}
		scriptWait("ellipsoid ID " + id + " " + m.getHighlightColorString() + " color translucent " + trans);
		if (showLastHighlightColor && m.getLastHighlightColorString() != null)
			scriptWait("ellipsoid ID " + id + "_2 " + m.getLastHighlightColorString() + " color translucent "
					+ Math.max(0, (trans - 0.1)));
		else
			scriptWait("ellipsoid ID " + id + "_2 color translucent 1.0");
	}

	public synchronized void zap(boolean b, boolean c, boolean d)
	{
		viewer.zap(b, c, d);
	}

	public synchronized void loadCompoundFromFile(String s, String filename, String s2[], Object reader, boolean b,
			Hashtable<String, Object> t, StringBuffer sb, int i)
	{
		viewer.loadModelFromFile(s, filename, s2, reader, b, t, sb, i);
	}

	public synchronized int getCompoundCount()
	{
		return viewer.getModelCount();
	}

	public synchronized void setAtomCoordRelative(Vector3f c, BitSet bitSet)
	{
		viewer.setAtomCoordRelative(c, bitSet);
	}

	public synchronized void setAtomCoordRelative(final List<Vector3f> c, final List<BitSet> bitSet,
			final AnimationSpeed overlapAnim)
	{
		if (isAnimated() && c.size() > 1)
		{
			checkNoAWT();
			int n = (overlapAnim == AnimationSpeed.SLOW) ? 24 : 10;
			for (int i = 0; i < n; i++)
			{
				for (int j = 0; j < bitSet.size(); j++)
				{
					Vector3f v = new Vector3f(c.get(j));
					v.scale(1 / (float) n);
					viewer.setAtomCoordRelative(v, bitSet.get(j));
				}
				viewer.scriptWait("delay 0.01");
			}
		}
		else
		{
			for (int i = 0; i < bitSet.size(); i++)
				viewer.setAtomCoordRelative(c.get(i), bitSet.get(i));
		}
	}

	public synchronized void setAtomProperty(BitSet bitSet, int temperature, int v, float v2, String string, float f[],
			String s[])
	{
		viewer.setAtomProperty(bitSet, temperature, v, v2, string, f, s);
	}

	public synchronized int getAtomCountInCompound(int index)
	{
		return viewer.getAtomCountInModel(index);
	}

	public synchronized BitSet getSmartsMatch(String smarts, BitSet bitSet)
	{
		BitSet b = viewer.getSmartsMatch(smarts, bitSet);
		if (b == null)
		{
			Settings.LOGGER.warn("jmol did not like: " + smarts + " " + bitSet);
			return new BitSet();
		}
		else
			return b;
	}

	HashSet<String> animSuspend = new HashSet<String>();

	public synchronized void suspendAnimation(String key)
	{
		if (animSuspend.contains(key))
			throw new Error("already suspended animation for: " + key);
		animSuspend.add(key);
	}

	public synchronized void proceedAnimation(String key)
	{
		if (!animSuspend.contains(key))
			throw new Error("use suspend first for " + key);
		animSuspend.remove(key);
	}

	public synchronized boolean isAnimated()
	{
		return guiControler.isVisible() && animSuspend.size() == 0;
	}

	public synchronized void hideHydrogens(boolean b)
	{
		scriptWait("set showHydrogens " + (b ? "FALSE" : "TRUE"));
	}

	public float getDiameter(BitSet bitSet)
	{
		List<Vector3f> points = new ArrayList<Vector3f>();
		for (int i = 0; i < bitSet.size(); i++)
			if (bitSet.get(i))
				points.add(new Vector3f(viewer.getAtomPoint3f(i)));
		Vector3f[] a = new Vector3f[points.size()];
		return Vector3fUtil.maxDist(points.toArray(a));
	}

	//	public List<Vector3f> getCenterAndAxes(BitSet bitSet)
	//	{
	//		BoxInfo info = viewer.getBoxInfo(bitSet, 1.0F);
	//		Point3f center = info.getBoundBoxCenter();
	//		Vector3f corner = info.getBoundBoxCornerVector();
	//
	////		System.out.println(info);
	////		System.out.println(corner);
	////		System.out.println(ArrayUtil.toString(info.getBboxVertices()));
	//
	//		return null;
	//	}

	HashMap<Dimension, Dimension> cachedResolutions = new HashMap<Dimension, Dimension>();

	/**
	 * Copied from org.openscience.jmol.app.jmolpanel.JmolPanel
	 */
	public void exportImage()
	{
		Dimension resScreen = new Dimension(viewer.getScreenWidth(), viewer.getScreenHeight());
		Dimension resCached = cachedResolutions.get(resScreen);
		if (resCached == null)
			resCached = resScreen;
		Dimension resSelected = ResolutionPanel.getResuloution(Settings.TOP_LEVEL_FRAME, "Select Image Resolution",
				(int) resCached.getWidth(), (int) resCached.getHeight());
		if (resSelected == null)
			return;
		cachedResolutions.put(resScreen, resSelected);

		int qualityJPG = -1;
		int qualityPNG = -1;
		String imageType = null;
		String[] imageChoices = { "JPEG", "PNG", "GIF", "PPM", "PDF" };
		String[] imageExtensions = { "jpg", "png", "gif", "ppm", "pdf" };
		Dialog sd = new Dialog();
		String dir = PropHandler.get("image-export-dir");
		if (dir == null)
			dir = System.getProperty("user.home");
		String name = dir + File.separator + "ches-mapper-image.jpg";
		String fileName = sd.getImageFileNameFromDialog(viewer, name, imageType, imageChoices, imageExtensions,
				qualityJPG, qualityPNG);
		if (fileName == null)
			return;
		PropHandler.put("image-export-dir", FileUtil.getParent(fileName));
		PropHandler.storeProperties();
		qualityJPG = sd.getQuality("JPG");
		qualityPNG = sd.getQuality("PNG");
		String sType = imageType = sd.getType();
		if (sType == null)
		{
			// file type changer was not touched
			sType = fileName;
			int i = sType.lastIndexOf(".");
			if (i < 0)
				return; // make no assumptions - require a type by extension
			sType = sType.substring(i + 1).toUpperCase();
		}
		Settings.LOGGER.info((String) viewer.createImage(fileName, sType, null, sd.getQuality(sType),
				resSelected.width, resSelected.height));
	}

	private int getFirstCarbonAtom(BitSet bs)
	{
		//System.out.println(empty.cardinality());
		int firstAtom = -1;
		int firstCarbon = -1;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
		{
			//System.out.println(i + " " + viewer.getAtomInfo(i));
			if (firstAtom == -1)
				firstAtom = i;
			if (viewer.getAtomInfo(i).matches("^C[0-9].*"))
			{
				firstCarbon = i;
				break;
			}
		}
		if (firstCarbon == -1)
			firstCarbon = firstAtom;
		//		System.out.println("atom to select: " + viewer.getAtomInfo(firstCarbon));

		return firstCarbon;
	}

	public BitSet getDotModeHideBitSet(BitSet bs)
	{
		BitSet set = new BitSet(bs.length());
		set.or(bs);
		set.clear(getFirstCarbonAtom(bs));
		return set;
	}

	public BitSet getDotModeDisplayBitSet(BitSet bs)
	{
		BitSet set = new BitSet(bs.length());
		set.set(getFirstCarbonAtom(bs));
		return set;
	}

	public synchronized void selectFirstCarbonAtom(BitSet bs)
	{
		BitSet sel = new BitSet(bs.length());
		sel.set(getFirstCarbonAtom(bs));
		select(sel);
	}

}
