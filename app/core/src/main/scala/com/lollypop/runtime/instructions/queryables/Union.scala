package com.lollypop.runtime.instructions.queryables

import com.lollypop.language.HelpDoc.{CATEGORY_DATAFRAMES_IO, PARADIGM_DECLARATIVE}
import com.lollypop.language.SQLTemplateParams.MappedParameters
import com.lollypop.language.models.Queryable
import com.lollypop.language.{HelpDoc, QueryableChainParser, SQLCompiler, SQLTemplateParams, TokenStream}
import com.lollypop.runtime.devices.RowCollection
import com.lollypop.runtime.instructions.queryables.AssumeQueryable.EnrichedAssumeQueryable
import com.lollypop.runtime.{LollypopVM, Scope}
import lollypop.io.IOCost

/**
 * Represents a union operation; which combines two queries.
 * @param query0 the first [[Queryable queryable resource]]
 * @param query1 the second [[Queryable queryable resource]]
 */
case class Union(query0: Queryable, query1: Queryable) extends RuntimeQueryable {

  override def search()(implicit scope: Scope): (Scope, IOCost, RowCollection) = {
    val (scopeA, costA, deviceA) = LollypopVM.search(scope, query0)
    val (scopeB, costB, deviceB) = LollypopVM.search(scopeA, query1)
    (scopeB, costA ++ costB, deviceA union deviceB)
  }

  override def toSQL: String = s"${query0.toSQL} union ${query1.toSQL}"

}

object Union extends QueryableChainParser {
  private val template = "union ?%C(mode|all|distinct) %Q:query"

  override def parseQueryableChain(ts: TokenStream, host: Queryable)(implicit compiler: SQLCompiler): Queryable = {
    val params = SQLTemplateParams(ts, template)
    val isDistinct = params.atoms.is("mode", _.name == "distinct")
    if (isDistinct) UnionDistinct(query0 = host, query1 = params.instructions("query").asQueryable)
    else Union(query0 = host, query1 = params.instructions("query").asQueryable)
  }

  override def help: List[HelpDoc] = List(HelpDoc(
    name = "union",
    category = CATEGORY_DATAFRAMES_IO,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = "%q:query0 union ?%C(mode|all|distinct) %q:query1",
    description = "Combines two (or more) result sets (vertically)",
    example =
      """|from (
         |  |------------------------------|
         |  | symbol | exchange | lastSale |
         |  |------------------------------|
         |  | AAXX   | NYSE     |    56.12 |
         |  | UPEX   | NYSE     |   116.24 |
         |  | XYZ    | AMEX     |    31.95 |
         |  |------------------------------|
         |) union (
         |  |------------------------------|
         |  | symbol | exchange | lastSale |
         |  |------------------------------|
         |  | JUNK   | AMEX     |    97.61 |
         |  | ABC    | OTC BB   |    5.887 |
         |  |------------------------------|
         |)
         |""".stripMargin
  ), HelpDoc(
    name = "union distinct",
    category = CATEGORY_DATAFRAMES_IO,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = "%q:query0 union distinct distinct %q:query1",
    description = "Combines two (or more) result sets (vertically) retaining only distinct rows",
    example =
      """|from (
         |    |------------------------------|
         |    | symbol | exchange | lastSale |
         |    |------------------------------|
         |    | AAXX   | NYSE     |    56.12 |
         |    | UPEX   | NYSE     |   116.24 |
         |    | XYZ    | AMEX     |    31.95 |
         |    | ABC    | OTCBB    |    5.887 |
         |    |------------------------------|
         |) union distinct (
         |    |------------------------------|
         |    | symbol | exchange | lastSale |
         |    |------------------------------|
         |    | JUNK   | AMEX     |    97.61 |
         |    | AAXX   | NYSE     |    56.12 |
         |    | ABC    | OTCBB    |    5.887 |
         |    |------------------------------|
         |)
         |""".stripMargin
  ))

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "union"

}