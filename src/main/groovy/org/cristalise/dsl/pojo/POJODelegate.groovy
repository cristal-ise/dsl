/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.pojo

import org.codehaus.groovy.runtime.StringBufferWriter
import org.cristalise.dsl.persistency.database.DatabaseType
import org.cristalise.dsl.persistency.outcome.Struct
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.utils.Logger
/**
 *
 */
class POJODelegate {

    List<String> importLines = []
    String packageLine;
	String uuidField = "UUID"
    String pojoString

    
	public POJODelegate(String packageName) {
		this.packageLine = "package " + packageName + ";\n"
	}
	
	/**
	 *
	 * @param name
	 * @param cl
	 */
    void processClosure(String name, Closure cl) {

        assert cl, "POJO only works with a valid Closure"

        Logger.msg 1, "POJO(start) ---------------------------------------"

        def objBuilder = new ObjectGraphBuilder()
        objBuilder.classLoader = this.class.classLoader
        objBuilder.classNameResolver = 'org.cristalise.dsl.persistency.outcome'
        cl.delegate = objBuilder
        buildPOJO(name, cl())

        Logger.msg 1, "POJO(end) +++++++++++++++++++++++++++++++++++++++++"
    }

    /**
     *
     * @param name
     * @param s
     */
    void buildPOJO(String name, Struct s) {
        if (!s) throw new InvalidDataException("POJO cannot be built from empty declaration")
        def classLine = "public class ${name} {\n"
        def properties = buildProperties(s)

        // construct pojo
        pojoString = buildPOJOContent(classLine, properties);
    }

    /**
     *
     * @param s
     * @return
     */
    private List<String> buildProperties(Struct s) {
        Logger.msg 1, "POJODelegate.buildStruct() - Struct: $s.name"

        def properties = new ArrayList()
        if (s.fields || s.structs || s.anyField) {
            properties.add(uuidField)
            s.fields.each {
                def name = "${it.name}".toUpperCase()
                def type = getJavaFieldType(it.type)
				def property = type + ":" + name
                properties << property
            }
        }

        return properties

    }

    /**
     *
     * @param structType
     * @return
     * @throws IOException
     */
    private String getJavaFieldType(String structType) throws IOException {

        def fieldType
        if (structType.contains(":")) {
            fieldType = structType.split(":")[1]
        }

        switch (fieldType.toLowerCase()) {
            case "double":
                return "Double"
            case "int":
            case "integer":
                return "Integer"
            case "uuid":
				importLines << "import java.util.UUID;\n"
                return "UUID"
            case "boolean":
                return "Boolean"
            case "float":
                return "Float"
            case "long":
                return "Long"
            case "byte":
                return "Byte.class"
            case "char":
            case "character":
                return "Character"
            case "short":
                return "Short"
            case "datetime":
            case "date":
				importLines << "import java.util.Date;\n"
                return "Date"
            case "string":
                return "String"
            case "decimal":
				importLines << "import java.math.BigDecimal;\n"
                return "BigDecimal"
            default:
                throw new IOException("Invalid field data type. '${fieldType}'")
        }
    }

    /**
     *
     * @param classLine
     * @param properties
     * @return
     */
    private String buildPOJOContent(String classLine, List<String> properties) {
        def pojoBuffer = new StringBuffer()
        def writer = new StringBufferWriter(pojoBuffer)
		
		writer.append(packageLine)
		writer.append("\n")
		
		importLines.each { 
			writer.append(it)
		}
		
		writer.append("\n")
		writer.append(classLine)
		
		properties.each { 
			def propertyTypeName = it.split(":")
			def type = propertyTypeName[0]
			def name = propertyTypeName[1]
			def camelCaseName = name[0].toLowerCase() + name.substring(1)
			
			writer.append("\tprivate " + type + " " + camelCaseName + ";\n")
		}
		
		writer.append("\n")
		
		properties.each { 
			def propertyTypeName = it.split(":")
			def type = propertyTypeName[0]
			def name = propertyTypeName[1]
			def camelCaseName = name[0].toLowerCase() + name.substring(1)
			
			writer.append("public void set" + name + "(" + type + " " + camelCaseName + ") {\n")
			writer.append("this." + camelCaseName + " = " + camelCaseName + ";\n")
			writer.append("}\n")
			
			writer.append("public " + type + " get" + name + "() {\n")
			writer.append("return this." + camelCaseName + ";\n")
			writer.append("}\n")
		}
		
		writer.append("}")
		
        return writer.toString()
    }


}
