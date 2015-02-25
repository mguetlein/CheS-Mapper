
## CheS-Mapper

CheS-Mapper is a 3D viewer for small molecule datasets.

For documentation and to run CheS-Mapper, please visit: http://ches-mapper.org

#### Build

To build the project form source use maven:

	mvn clean install

To include it into your own java maven project:

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



    
