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
package org.cristalise.dsl.collection

import org.cristalise.dsl.property.PropertyBuilder
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.collection.DependencyDescription
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.process.Gateway

import groovy.transform.CompileStatic
import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.utils.Logger


/**
 * 
 *
 */
@CompileStatic
class DependencyDelegate {
    Dependency dependency

    public DependencyDelegate(String n, boolean isDescription) {
        dependency = isDescription ? new DependencyDescription(n) : new Dependency(n)
    }

    public void  processClosure(Closure cl) {
        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void Properties(Closure cl) {
        dependency.properties = PropertyBuilder.build(cl)
    }

    public void Member(Map attrs, Closure cl = null) {
        assert attrs && attrs.itemPath

        String iPathStr
        ItemPath itemPath = new ItemPath()

        if (attrs.itemPath instanceof PropertyDescriptionList) {
            def propDesc = (PropertyDescriptionList)attrs.itemPath
            //FIXME module namespace is hardcoded
            iPathStr = "/desc/Property/testns/${propDesc.name}"
        }
        else 
            iPathStr = (String)attrs.itemPath

        assert iPathStr

        try {
            itemPath = Gateway.getLookup().resolvePath(new DomainPath(iPathStr))
        }
        catch (Exception e) {
            Logger.warning "Unable to find the domain path. ${e.localizedMessage}"
        }

        //NOTE: this is a kind of hack so the DSL 
        if (!ItemPath.isUUID(iPathStr)) itemPath.path[0] = iPathStr

        def member = dependency.addMember(itemPath)

        if(cl) {
            DependencyMemberDelegate delegate = new DependencyMemberDelegate()
            delegate.processClosure(cl)
            member.properties << delegate.props
        }
    }
}
