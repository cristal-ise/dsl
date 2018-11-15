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
	 * @param pojoFile
	 * @return
	 */
	public POJOBuilder loadPOJO(String pojoFile) {
		Logger.msg 5, "POJOBuilder.loadPOJO() - From file:$pojoFile"

		pojo = new POJO(name, new File(pojoFile).text)
		
		return this
	}

    /**
     *
     * @param name
     * @param cl
     * @return
     */
    public static POJO build(String name, Closure cl, String packageName) {
        def pojo = new POJOBuilder(name)
        
        generatePOJO(pojo, cl, packageName)
        
        return pojo.pojo
    }

    private static void generatePOJO(POJOBuilder pojoBuilder, Closure cl, String packageName) {
        def pojoDelegate = new POJODelegate(packageName)
        try {
            pojoDelegate.processClosure(pojoBuilder.name, cl)
            Logger.msg 5, "POJOBuilder - generated pojo data:\n" + pojoDelegate.pojoString

            pojoBuilder.pojo = new POJO(pojoBuilder.name, pojoDelegate.pojoString)
        } catch (Exception e) {
            throw new InvalidDataException(e.getMessage())
        }

    }

    /**
     *
     * @param name
     * @param pojoFile
     * @return
     */
    public static POJOBuilder build(String name, String pojoFile) {
        def pb = new POJOBuilder(name)
        return pb.loadPOJO(pojoFile)
    }
}
