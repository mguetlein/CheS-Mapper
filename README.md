
## CheS-Mapper

CheS-Mapper is a 3D viewer for small molecule datasets.

For documentation and to run CheS-Mapper, please visit: http://ches-mapper.org

#### Build

Please use maven to compile the project from source code:

	mvn clean install

You can also include it into your own java maven project:

```
<repository>
	<id>jgu</id>
	<name>jgu</name>
	<url>http://fantomas.informatik.uni-mainz.de/mvnrepo/repository</url>
</repository>
```
```
<dependency>
	<groupId>org.chesmapper</groupId>
	<artifactId>CheS-Mapper</artifactId>
	<version>X.X.X</version>
</dependency>
```

Replace X.X.X with the version of your choice. Check http://fantomas.informatik.uni-mainz.de/mvnrepo/repository/org/chesmapper/CheS-Mapper/maven-metadata-local.xml for available versions.

#### Source code

This git repository includes all 3d viewer related source code.

The mapping and wizard code is provided in sub-project: https://github.com/mguetlein/CMMapping




    
