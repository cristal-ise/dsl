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

    /**
     *
     * @param name
     * @param cl
     */
	public POJODelegate(String packageName) {
		this.packageLine = "package " + packageName + ";\n"
	}
	
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
        def pojoBuffer = new StringBuffer()
        def writer = new StringBufferWriter(pojoBuffer)
        writer.append(importLines)

        def fields = buildFields(writer, s)
        String commonLines = writer.toString()

        // construct pojo
        pojoString = buildScriptContent(tableName, commonLines, fields, DatabaseType.CREATE);
    }

    /**
     *
     * @param w
     * @param s
     * @return
     */
    private List<String> buildFields(StringBufferWriter w, Struct s) {
        Logger.msg 1, "POJODelegate.buildStruct() - Struct: $s.name"

        def jqFields = new ArrayList()
        if (s.fields || s.structs || s.anyField) {
            w.append("def ${uuidField} = field(name('${uuidField}'), UUID.class)\n")
            jqFields.add(uuidField)
            s.fields.each {
                def fieldName = "${it.name}".toUpperCase()
                def type = getJooqFieldTye(it.type)
                w.append("def ${fieldName} = field(name('${fieldName}'), ${type})\n")
                jqFields << it.name
            }
        }

        return jqFields

    }

    /**
     *
     * @param structType
     * @return
     * @throws IOException
     */
    private String getJooqFieldTye(String structType) throws IOException {

        def fieldType
        if (structType.contains(":")) {
            fieldType = structType.split(":")[1]
        }

        switch (fieldType.toLowerCase()) {
            case "double":
                return "Double.class"
            case "int":
            case "integer":
                return "Integer.class"
            case "uuid":
                return "UUID.class"
            case "boolean":
                return "Boolean.class"
            case "float":
                return "Float.class"
            case "long":
                return "Long.class"
            case "byte":
                return "Byte.class"
                break
            case "char":
            case "character":
                return "Character.class"
            case "short":
                return "Short.class"
            case "datetime":
                return "Timestamp.class"
            case "date":
                return "Date.class"
            case "string":
                return "String.class"
            case "decimal":
                return "BigDecimal.class"
            default:
                throw new IOException("Invalid field data type. '${fieldType}'")
        }
    }

    /**
     *
     * @param name
     * @param commonLines
     * @param fields
     * @param type
     * @return
     */
    private String buildScriptContent(String name, String commonLines, List<String> fields, DatabaseType type) {

        def w = new StringBufferWriter(new StringBuffer(commonLines + "\n"))

        if (type == DatabaseType.CREATE) { // create table content

            w.append("dsl.createTableIfNotExists(${name})\n")
            fields.each {
                w.append("        .column(${it.toUpperCase()})\n")
            }
            w.append("        .constraints(constraint('PK_' + ${name}).primaryKey(${uuidField}))\n")
            w.append("        .execute()\n")

        } else if (type == DatabaseType.INSERT) { // insert query content

            w.append("def insertQueryResult = dsl.insertQuery(${name})\n")
            fields.each {
                if (it == uuidField){
                    w.append("insertQueryResult.addValue(${it.toUpperCase()}, uuid)\n")
                }else {
                    w.append("insertQueryResult.addValue(${it.toUpperCase()}, outcome.getField('${it}'))\n")
                }
            }
            w.append("insertQueryResult.onDuplicateKeyUpdate(true)\n")
            w.append("insertQueryResult.onConflict(${uuidField})\n")
            fields.each {
                if (it != uuidField){
                    w.append("insertQueryResult.addValueForUpdate(${it.toUpperCase()}, outcome.getField('${it}'))\n")
                }
            }
            w.append("def result = insertQueryResult.execute()\n\n")
            w.append("result")

        } else if (type == DatabaseType.SELECT) { //select query content
            w.append("def result = dsl.select()\n")
            w.append("        .from(${name})\n")
            w.append("        .where(${uuidField}.equal(uuid))\n")
            w.append("        .fetchOne()\n\n")
            w.append("result")
        } else if (type == DatabaseType.SELECT_ALL) { //select query content
            w.append("def result = dsl.select()\n")
            w.append("        .from(${name})\n")
            w.append("        .where(${uuidField}.equal(uuid))\n")
            w.append("        .fetch()\n\n")
            w.append("result")
        } else if (type == DatabaseType.UPDATE) { // update query content
            w.append("def updateQueryResult = dsl.updateQuery(${name})\n")
            fields.each {
                if (it != uuidField) {
                    w.append("updateQueryResult.addValue(${it.toUpperCase()}, outcome.getField('${it}'))\n")
                }
            }
            w.append("updateQueryResult.addConditions(${uuidField}.equal(uuid))\n")
            w.append("def result = updateQueryResult.execute()\n\n")
            w.append("result")
        } else if (type == DatabaseType.DELETE) {
            w.append("def result = dsl.delete(${name})\n")
            w.append("        .where(${uuidField}.equal(uuid))\n")
            w.append("        .execute()\n\n")
            w.append("result")
        }

        return w.toString()
    }

    /**
     * Creates the script items for the domain.
     * @param name
     * @return
     */
    private String getDomainScript(String name){
        String itemNameCreate = name + DatabaseType.CREATE.getValue()
        String itemNameInsert = name + DatabaseType.INSERT.getValue()
        String itemNameUpdate = name + DatabaseType.UPDATE.getValue()
        String itemNameSelect = name + DatabaseType.SELECT.getValue()
        String itemNameDelete = name + DatabaseType.DELETE.getValue()
        StringBufferWriter sbw = new StringBufferWriter(new StringBuffer())
                .append("Script('${itemNameCreate}', 0) {\n")
                .append("    input('dsl', 'org.jooq.DSLContext')\n")
                .append("    script('groovy', 'src/main/script/DB/${itemNameCreate}.groovy')\n")
                .append("}\n\n")
                .append("Script('${itemNameInsert}', 0) {\n")
                .append("    input('dsl', 'org.jooq.DSLContext')\n")
                .append("    input('outcome', 'org.cristalise.kernel.persistency.outcome.Outcome')\n")
                .append("    input('uuid', 'java.util.UUID')\n")
                .append("    output('result', 'java.lang.Integer')\n")
                .append("    script('groovy', 'src/main/script/DB/${itemNameInsert}.groovy')\n")
                .append("}\n\n")
                .append("Script('${itemNameUpdate}', 0) {\n")
                .append("    input('dsl', 'org.jooq.DSLContext')\n")
                .append("    input('outcome', 'org.cristalise.kernel.persistency.outcome.Outcome')\n")
                .append("    input('uuid', 'java.util.UUID')\n")
                .append("    output('result', 'java.lang.Integer')\n")
                .append("    script('groovy', 'src/main/script/DB/${itemNameUpdate}.groovy')\n")
                .append("}\n\n")
                .append("Script('${itemNameSelect}', 0) {\n")
                .append("    input('dsl', 'org.jooq.DSLContext')\n")
                .append("    input('uuid', 'java.util.UUID')\n")
                .append("    output('result', 'org.jooq.Record')\n")
                .append("    script('groovy', 'src/main/script/DB/${itemNameSelect}.groovy')\n")
                .append("}\n\n")
                .append("Script('${itemNameDelete}', 0) {\n")
                .append("    input('dsl', 'org.jooq.DSLContext')\n")
                .append("    input('uuid', 'java.util.UUID')\n")
                .append("    output('result', 'java.lang.Integer')\n")
                .append("    script('groovy', 'src/main/script/DB/${itemNameDelete}.groovy')\n")
                .append("}\n\n")
    }

}