package com.lollypop.runtime.instructions.jvm

import com.lollypop.language.HelpDoc.{CATEGORY_JVM_REFLECTION, PARADIGM_OBJECT_ORIENTED}
import com.lollypop.language.models.Expression
import com.lollypop.language.{ExpressionChainParser, HelpDoc, SQLCompiler, TokenStream}
import com.lollypop.runtime.instructions.expressions.{NamedFunctionCall, RuntimeExpression}
import com.lollypop.runtime.plastics.RuntimeClass.implicits.RuntimeClassInstanceSugar
import com.lollypop.runtime.{LollypopVM, Scope}
import com.lollypop.util.OptionHelper.OptionEnrichment

/**
 * Invoke a Virtual Method
 * @example {{{
 *  val items = values ("NASDAQ", 1276), ("AMEX", 1259), ("NYSE", 1275), ("OTCBB", 1190)
 *  items.!toTable()
 * }}}
 */
case class InvokeVirtualMethod(instance: Expression, method: Expression) extends RuntimeExpression {

  override def evaluate()(implicit scope: Scope): Any = {
    method match {
      case NamedFunctionCall(name, args) =>
        val inst = LollypopVM.execute(scope, instance)._3
        inst.invokeVirtualMethod(name, args)
      case x => x.dieIllegalType()
    }
  }

  override def toSQL: String = Seq(instance.wrapSQL, ".!", method.wrapSQL).mkString

}

object InvokeVirtualMethod extends ExpressionChainParser {
  private val __name = ".!"

  override def help: List[HelpDoc] = List(HelpDoc(
    name = __name,
    category = CATEGORY_JVM_REFLECTION,
    paradigm = PARADIGM_OBJECT_ORIENTED,
    syntax = "",
    description = "Invokes a virtual method",
    example =
      """|val items = values ("NASDAQ", 1276), ("AMEX", 1259), ("NYSE", 1275), ("OTCBB", 1190)
         |items.!toTable()
         |""".stripMargin
  ))

  override def parseExpressionChain(ts: TokenStream, host: Expression)(implicit compiler: SQLCompiler): Option[Expression] = {
    if (ts nextIf __name) {
      compiler.nextExpression(ts).map(InvokeVirtualMethod(host, _)) ?? ts.dieExpectedIdentifier()
    } else None
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is __name

}