package com.bryghts.ftypes

import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import com.bryghts.macros.helpers._

//import scala.annotation.compileTimeOnly
//import scala.reflect.macros.whitebox

//@compileTimeOnly("enable macro paradise to expand macro annotations")
class Async extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro AsyncMacro.impl
}


object AsyncMacro {
    def impl(cs: MacroHelpers.Context)(annottees: cs.Expr[Any]*): cs.Expr[Any] = {
        val helpers = MacroHelpers(cs)
        import helpers._
        import c.universe._

        def extractCaseClassesParts(classDecl: ClassDef) = classDecl match {
            case q"case class $className(..$fields) extends ..$parents { ..$body }" =>
                (className, fields, parents, body)
        }

        def modifiedDeclaration(classDecl: ClassDef) = {
            val (className, fields, parents, body) = extractCaseClassesParts(classDecl)

            val params = fields.asInstanceOf[List[ValDef]] map { p => p.duplicate}

            val name: c.universe.Name = className.asInstanceOf[c.universe.TypeName].decodedName
            val termName : TermName = termNameFor(name.toString)
            val typeName : TypeName = typeNameFor(name.toString)
            val types = params.map(_.tpt).distinct.zipWithIndex
            val flattenerTypes = types.map{case (n, i) =>
                ValDef(
                    Modifiers(Flag.IMPLICIT | Flag.PARAM),
                    termNameFor("flattener" + i),
                    AppliedTypeTree(
                        Select( Select( Select( Select(Ident(termNameFor("com")), termNameFor("bryghts")), termNameFor("ftypes")), termNameFor("async")), typeNameFor("Flattener")),
                        List(n)),
                    EmptyTree)
            }
            val implicitApplyParams = params.map(_.tpt).distinct.zipWithIndex.map{case (n, i) =>
                ValDef(
                    Modifiers(Flag.IMPLICIT | Flag.PARAM),
                    termNameFor("flattener" + i),
                    AppliedTypeTree(
                        Select(Select(Select(Select(Ident(termNameFor("com")), termNameFor("bryghts")), termNameFor("ftypes")), termNameFor("async")), typeNameFor("Flattener")),
                        List(n)),
                    EmptyTree)
            }
            val flattenerParamNames =(0 until implicitApplyParams.length).toList.map{n => termNameFor("flattener" + n.toString)}
            val paramNames = params.map(_.name)
            val typesMap = types.toMap.mapValues(n => termNameFor("flattener" + n))

            val extractors = params.map{p =>
                val n = p.name
                val t = p.tpt

                q"""
                    def $n:$t = ${typesMap(t)}.flatten(this.future.map(_.$n))
                 """
            }

            c.Expr[Any](
                q"""
                case class $typeName private(                    val future:           scala.concurrent.Future[$termName.sync])
                                            ( implicit protected val executionContext: scala.concurrent.ExecutionContext, ..$flattenerTypes)  extends com.bryghts.ftypes.async.Any[$termName.sync, $typeName]{
                  ..$extractors
                  ..$body
                }
                object $termName {
                    case class sync ( ..$params ) extends ..$parents { }
                    def apply(..$params)(implicit executionContext: scala.concurrent.ExecutionContext, ..$implicitApplyParams): $typeName =
                        new $typeName(
                            scala.concurrent.Future.successful(sync(..$paramNames))
                        )(executionContext, ..$flattenerParamNames)

                    implicit val flattener : com.bryghts.ftypes.async.Flattener[$typeName] = new com.bryghts.ftypes.async.Flattener[$typeName] {
                        override def flatten(in: scala.concurrent.Future[$typeName])(implicit executionContext: scala.concurrent.ExecutionContext): $typeName =
                            new $typeName(
                                in.flatMap(_.future)
                            )
                    }
                }
              """
            )
        }
        annottees map (_.tree) toList match {
            case (classDecl: cs.universe.ClassDef) :: Nil => modifiedDeclaration(classDecl.asInstanceOf[c.universe.ClassDef]).asInstanceOf[cs.Expr[Any]]
            case _ => c.abort(c.enclosingPosition, "Invalid annottee")
        }
    }
}
