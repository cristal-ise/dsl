/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.pojo

import groovy.transform.CompileStatic
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.utils.Logger

/**
 *
 */
@CompileStatic
class POJOBuilder {
    String name = ""

    POJO pojo = null

    public POJOBuilder() {}

    /**
     *
     * @param name
     */
    public POJOBuilder(String name) {
        this.name = name
    }

    /**
     *
     * @param name
     * @param cl
     * @return
     */
    public static POJO build(String name, Closure cl) {
        def pojo = new POJOBuilder(name)
        
        generatePOJO(pojo, cl)
        
        return pojo.pojo
    }

    private static void generatePOJO(POJOBuilder pojoBuilder, Closure cl) {
        def pojoDelegate = new DatabaseDelegate()
        try {
            pojoDelegate.processClosure(db.name, cl)
            Logger.msg 5, "POJOBuilder - generated pojo data:\n" + pojoDelegate.pojoData

            pojoBuilder.pojo = new POJO(pojoBuilder.name, pojoDelegate.pojoString)
        } catch (Exception e) {
            throw new InvalidDataException(e.getMessage())
        }

    }

    /**
     *
     * @param module
     * @param name
     * @param version
     * @param dbCreateFile
     * @param dbInsertFile
     * @param dbSelectFile
     * @param dbUpdateFile
     * @return
     */
    public static POJOBuilder build(String module, String name, int version, String dbCreateFile, String dbInsertFile,
                                        String dbSelectFile, String dbSelectAllFile, String dbUpdateFile, String dbDeleteFile, String dbScriptFile) {
        def sb = new POJOBuilder(module, name, version)
        return sb.loadDB(dbCreateFile, dbInsertFile, dbSelectFile, dbSelectAllFile, dbUpdateFile, dbDeleteFile, dbScriptFile)
    }
}
