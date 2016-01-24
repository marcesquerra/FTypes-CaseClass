package com.bryghts.macros.helpers

import scala.reflect.macros.whitebox.{Context => MContext}

/**
 * Created by Marc Esquerrà on 24/01/2016.
 */
case class MacroHelpers(c: MacroHelpers.Context) {

    def termNameFor(name: String) = c.universe.TermName(name)
    def typeNameFor(name: String) = c.universe.TypeName(name)

}

object MacroHelpers {
    type Context = MContext
}
