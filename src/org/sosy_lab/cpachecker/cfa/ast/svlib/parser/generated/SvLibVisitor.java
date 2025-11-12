// Generated from SvLib.g4 by ANTLR 4.13.2
package org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced by {@link
 * SvLibParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for operations with no return
 *     type.
 */
public interface SvLibVisitor<T> extends ParseTreeVisitor<T> {
  /**
   * Visit a parse tree produced by {@link SvLibParser#script}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitScript(SvLibParser.ScriptContext ctx);

  /**
   * Visit a parse tree produced by the {@code DeclareVar} labeled alternative in {@link
   * SvLibParser#commandSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDeclareVar(SvLibParser.DeclareVarContext ctx);

  /**
   * Visit a parse tree produced by the {@code DefineProc} labeled alternative in {@link
   * SvLibParser#commandSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDefineProc(SvLibParser.DefineProcContext ctx);

  /**
   * Visit a parse tree produced by the {@code AnnotateTag} labeled alternative in {@link
   * SvLibParser#commandSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAnnotateTag(SvLibParser.AnnotateTagContext ctx);

  /**
   * Visit a parse tree produced by the {@code SelectTrace} labeled alternative in {@link
   * SvLibParser#commandSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSelectTrace(SvLibParser.SelectTraceContext ctx);

  /**
   * Visit a parse tree produced by the {@code VerifyCall} labeled alternative in {@link
   * SvLibParser#commandSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitVerifyCall(SvLibParser.VerifyCallContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetWitness} labeled alternative in {@link
   * SvLibParser#commandSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetWitness(SvLibParser.GetWitnessContext ctx);

  /**
   * Visit a parse tree produced by the {@code SMTLIBv2Command} labeled alternative in {@link
   * SvLibParser#commandSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSMTLIBv2Command(SvLibParser.SMTLIBv2CommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code AssumeStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAssumeStatement(SvLibParser.AssumeStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code AssignStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAssignStatement(SvLibParser.AssignStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code SequenceStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSequenceStatement(SvLibParser.SequenceStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code AnnotatedStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAnnotatedStatement(SvLibParser.AnnotatedStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code CallStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCallStatement(SvLibParser.CallStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code ReturnStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitReturnStatement(SvLibParser.ReturnStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code LabelStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitLabelStatement(SvLibParser.LabelStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code GotoStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGotoStatement(SvLibParser.GotoStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code IfStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitIfStatement(SvLibParser.IfStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code WhileStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitWhileStatement(SvLibParser.WhileStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code BreakStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitBreakStatement(SvLibParser.BreakStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code ContinueStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitContinueStatement(SvLibParser.ContinueStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code HavocStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitHavocStatement(SvLibParser.HavocStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code ChoiceStatement} labeled alternative in {@link
   * SvLibParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitChoiceStatement(SvLibParser.ChoiceStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code TagAttribute} labeled alternative in {@link
   * SvLibParser#attributeSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTagAttribute(SvLibParser.TagAttributeContext ctx);

  /**
   * Visit a parse tree produced by the {@code TagProperty} labeled alternative in {@link
   * SvLibParser#attributeSvLib}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTagProperty(SvLibParser.TagPropertyContext ctx);

  /**
   * Visit a parse tree produced by the {@code CheckTrueProperty} labeled alternative in {@link
   * SvLibParser#property}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCheckTrueProperty(SvLibParser.CheckTruePropertyContext ctx);

  /**
   * Visit a parse tree produced by the {@code LiveProperty} labeled alternative in {@link
   * SvLibParser#property}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitLiveProperty(SvLibParser.LivePropertyContext ctx);

  /**
   * Visit a parse tree produced by the {@code NotLiveProperty} labeled alternative in {@link
   * SvLibParser#property}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitNotLiveProperty(SvLibParser.NotLivePropertyContext ctx);

  /**
   * Visit a parse tree produced by the {@code RequiresProperty} labeled alternative in {@link
   * SvLibParser#property}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitRequiresProperty(SvLibParser.RequiresPropertyContext ctx);

  /**
   * Visit a parse tree produced by the {@code EnsuresProperty} labeled alternative in {@link
   * SvLibParser#property}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitEnsuresProperty(SvLibParser.EnsuresPropertyContext ctx);

  /**
   * Visit a parse tree produced by the {@code InvariantProperty} labeled alternative in {@link
   * SvLibParser#property}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitInvariantProperty(SvLibParser.InvariantPropertyContext ctx);

  /**
   * Visit a parse tree produced by the {@code DecreasesProperty} labeled alternative in {@link
   * SvLibParser#property}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDecreasesProperty(SvLibParser.DecreasesPropertyContext ctx);

  /**
   * Visit a parse tree produced by the {@code DecreasesLexProperty} labeled alternative in {@link
   * SvLibParser#property}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDecreasesLexProperty(SvLibParser.DecreasesLexPropertyContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#trace}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTrace(SvLibParser.TraceContext ctx);

  /**
   * Visit a parse tree produced by the {@code ChooseLocalVariableValue} labeled alternative in
   * {@link SvLibParser#step}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitChooseLocalVariableValue(SvLibParser.ChooseLocalVariableValueContext ctx);

  /**
   * Visit a parse tree produced by the {@code ChooseHavocVariableValue} labeled alternative in
   * {@link SvLibParser#step}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitChooseHavocVariableValue(SvLibParser.ChooseHavocVariableValueContext ctx);

  /**
   * Visit a parse tree produced by the {@code ChooseChoiceStatement} labeled alternative in {@link
   * SvLibParser#step}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitChooseChoiceStatement(SvLibParser.ChooseChoiceStatementContext ctx);

  /**
   * Visit a parse tree produced by the {@code NormalRelationalTerm} labeled alternative in {@link
   * SvLibParser#relationalTerm}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitNormalRelationalTerm(SvLibParser.NormalRelationalTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code OldRelationalTerm} labeled alternative in {@link
   * SvLibParser#relationalTerm}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitOldRelationalTerm(SvLibParser.OldRelationalTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code ApplicationRelationalTerm} labeled alternative in
   * {@link SvLibParser#relationalTerm}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitApplicationRelationalTerm(SvLibParser.ApplicationRelationalTermContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#procDeclarationArguments}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitProcDeclarationArguments(SvLibParser.ProcDeclarationArgumentsContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#start_}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitStart_(SvLibParser.Start_Context ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#generalReservedWord}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGeneralReservedWord(SvLibParser.GeneralReservedWordContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#simpleSymbol}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSimpleSymbol(SvLibParser.SimpleSymbolContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#quotedSymbol}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitQuotedSymbol(SvLibParser.QuotedSymbolContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#predefSymbol}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPredefSymbol(SvLibParser.PredefSymbolContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#predefKeyword}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPredefKeyword(SvLibParser.PredefKeywordContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#symbol}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSymbol(SvLibParser.SymbolContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#numeral}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitNumeral(SvLibParser.NumeralContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#decimal}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDecimal(SvLibParser.DecimalContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#hexadecimal}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitHexadecimal(SvLibParser.HexadecimalContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#binary}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitBinary(SvLibParser.BinaryContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#string}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitString(SvLibParser.StringContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#keyword}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitKeyword(SvLibParser.KeywordContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#spec_constant}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSpec_constant(SvLibParser.Spec_constantContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#s_expr}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitS_expr(SvLibParser.S_exprContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#index}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitIndex(SvLibParser.IndexContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#identifier}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitIdentifier(SvLibParser.IdentifierContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#attribute_value}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAttribute_value(SvLibParser.Attribute_valueContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#attribute}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAttribute(SvLibParser.AttributeContext ctx);

  /**
   * Visit a parse tree produced by the {@code SimpleSort} labeled alternative in {@link
   * SvLibParser#sort}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSimpleSort(SvLibParser.SimpleSortContext ctx);

  /**
   * Visit a parse tree produced by the {@code ParametricSort} labeled alternative in {@link
   * SvLibParser#sort}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitParametricSort(SvLibParser.ParametricSortContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#qual_identifer}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitQual_identifer(SvLibParser.Qual_identiferContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#var_binding}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitVar_binding(SvLibParser.Var_bindingContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#sorted_var}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSorted_var(SvLibParser.Sorted_varContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#pattern}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPattern(SvLibParser.PatternContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#match_case}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitMatch_case(SvLibParser.Match_caseContext ctx);

  /**
   * Visit a parse tree produced by the {@code SpecConstantTerm} labeled alternative in {@link
   * SvLibParser#term}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSpecConstantTerm(SvLibParser.SpecConstantTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code QualIdentifierTerm} labeled alternative in {@link
   * SvLibParser#term}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitQualIdentifierTerm(SvLibParser.QualIdentifierTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code ApplicationTerm} labeled alternative in {@link
   * SvLibParser#term}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitApplicationTerm(SvLibParser.ApplicationTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code LetTerm} labeled alternative in {@link
   * SvLibParser#term}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitLetTerm(SvLibParser.LetTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code ForallTerm} labeled alternative in {@link
   * SvLibParser#term}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitForallTerm(SvLibParser.ForallTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code ExistsTerm} labeled alternative in {@link
   * SvLibParser#term}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitExistsTerm(SvLibParser.ExistsTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code MatchTerm} labeled alternative in {@link
   * SvLibParser#term}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitMatchTerm(SvLibParser.MatchTermContext ctx);

  /**
   * Visit a parse tree produced by the {@code AnnotatedTerm} labeled alternative in {@link
   * SvLibParser#term}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAnnotatedTerm(SvLibParser.AnnotatedTermContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#sort_symbol_decl}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSort_symbol_decl(SvLibParser.Sort_symbol_declContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#meta_spec_constant}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitMeta_spec_constant(SvLibParser.Meta_spec_constantContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#fun_symbol_decl}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitFun_symbol_decl(SvLibParser.Fun_symbol_declContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#par_fun_symbol_decl}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPar_fun_symbol_decl(SvLibParser.Par_fun_symbol_declContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#theory_attribute}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTheory_attribute(SvLibParser.Theory_attributeContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#theory_decl}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTheory_decl(SvLibParser.Theory_declContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#logic_attribue}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitLogic_attribue(SvLibParser.Logic_attribueContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#logic}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitLogic(SvLibParser.LogicContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#sort_dec}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSort_dec(SvLibParser.Sort_decContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#selector_dec}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSelector_dec(SvLibParser.Selector_decContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#constructor_dec}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitConstructor_dec(SvLibParser.Constructor_decContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#datatype_dec}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDatatype_dec(SvLibParser.Datatype_decContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#function_dec}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitFunction_dec(SvLibParser.Function_decContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#function_def}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitFunction_def(SvLibParser.Function_defContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#prop_literal}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitProp_literal(SvLibParser.Prop_literalContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_assert}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_assert(SvLibParser.Cmd_assertContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_checkSat}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_checkSat(SvLibParser.Cmd_checkSatContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_checkSatAssuming}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_checkSatAssuming(SvLibParser.Cmd_checkSatAssumingContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_declareConst}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_declareConst(SvLibParser.Cmd_declareConstContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_declareDatatype}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_declareDatatype(SvLibParser.Cmd_declareDatatypeContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_declareDatatypes}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_declareDatatypes(SvLibParser.Cmd_declareDatatypesContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_declareFun}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_declareFun(SvLibParser.Cmd_declareFunContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_declareSort}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_declareSort(SvLibParser.Cmd_declareSortContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_defineFun}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_defineFun(SvLibParser.Cmd_defineFunContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_defineFunRec}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_defineFunRec(SvLibParser.Cmd_defineFunRecContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_defineFunsRec}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_defineFunsRec(SvLibParser.Cmd_defineFunsRecContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_defineSort}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_defineSort(SvLibParser.Cmd_defineSortContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_echo}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_echo(SvLibParser.Cmd_echoContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_exit}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_exit(SvLibParser.Cmd_exitContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getAssertions}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getAssertions(SvLibParser.Cmd_getAssertionsContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getAssignment}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getAssignment(SvLibParser.Cmd_getAssignmentContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getInfo}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getInfo(SvLibParser.Cmd_getInfoContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getModel}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getModel(SvLibParser.Cmd_getModelContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getOption}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getOption(SvLibParser.Cmd_getOptionContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getProof}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getProof(SvLibParser.Cmd_getProofContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getUnsatAssumptions}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getUnsatAssumptions(SvLibParser.Cmd_getUnsatAssumptionsContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getUnsatCore}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getUnsatCore(SvLibParser.Cmd_getUnsatCoreContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_getValue}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_getValue(SvLibParser.Cmd_getValueContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_pop}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_pop(SvLibParser.Cmd_popContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_push}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_push(SvLibParser.Cmd_pushContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_reset}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_reset(SvLibParser.Cmd_resetContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_resetAssertions}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_resetAssertions(SvLibParser.Cmd_resetAssertionsContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_setInfo}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_setInfo(SvLibParser.Cmd_setInfoContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_setLogic}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_setLogic(SvLibParser.Cmd_setLogicContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#cmd_setOption}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCmd_setOption(SvLibParser.Cmd_setOptionContext ctx);

  /**
   * Visit a parse tree produced by the {@code AssertCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAssertCommand(SvLibParser.AssertCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code CheckSatCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCheckSatCommand(SvLibParser.CheckSatCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code CheckSatAssumingCommand} labeled alternative in
   * {@link SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCheckSatAssumingCommand(SvLibParser.CheckSatAssumingCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DeclareConstCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDeclareConstCommand(SvLibParser.DeclareConstCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DeclareDatatypeCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDeclareDatatypeCommand(SvLibParser.DeclareDatatypeCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DeclareDatatypesCommand} labeled alternative in
   * {@link SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDeclareDatatypesCommand(SvLibParser.DeclareDatatypesCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DeclareFunCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDeclareFunCommand(SvLibParser.DeclareFunCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DeclareSortCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDeclareSortCommand(SvLibParser.DeclareSortCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DefineFunCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDefineFunCommand(SvLibParser.DefineFunCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DefineFunRecCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDefineFunRecCommand(SvLibParser.DefineFunRecCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DefineFunsRecCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDefineFunsRecCommand(SvLibParser.DefineFunsRecCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code DefineSortCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDefineSortCommand(SvLibParser.DefineSortCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code EchoCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitEchoCommand(SvLibParser.EchoCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code ExitCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitExitCommand(SvLibParser.ExitCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetAssertionsCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetAssertionsCommand(SvLibParser.GetAssertionsCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetAssignmentCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetAssignmentCommand(SvLibParser.GetAssignmentCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetInfoCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetInfoCommand(SvLibParser.GetInfoCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetModelCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetModelCommand(SvLibParser.GetModelCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetOptionCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetOptionCommand(SvLibParser.GetOptionCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetProofCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetProofCommand(SvLibParser.GetProofCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetUnsatAssumptionsCommand} labeled alternative in
   * {@link SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetUnsatAssumptionsCommand(SvLibParser.GetUnsatAssumptionsCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetUnsatCoreCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetUnsatCoreCommand(SvLibParser.GetUnsatCoreCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code GetValueCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGetValueCommand(SvLibParser.GetValueCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code PopCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPopCommand(SvLibParser.PopCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code PushCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPushCommand(SvLibParser.PushCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code ResetCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitResetCommand(SvLibParser.ResetCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code ResetAssertionsCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitResetAssertionsCommand(SvLibParser.ResetAssertionsCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code SetInfoCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSetInfoCommand(SvLibParser.SetInfoCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code SetLogicCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSetLogicCommand(SvLibParser.SetLogicCommandContext ctx);

  /**
   * Visit a parse tree produced by the {@code SetOptionCommand} labeled alternative in {@link
   * SvLibParser#command}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSetOptionCommand(SvLibParser.SetOptionCommandContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#b_value}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitB_value(SvLibParser.B_valueContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#option}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitOption(SvLibParser.OptionContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#info_flag}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitInfo_flag(SvLibParser.Info_flagContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#error_behaviour}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitError_behaviour(SvLibParser.Error_behaviourContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#reason_unknown}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitReason_unknown(SvLibParser.Reason_unknownContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#model_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitModel_response(SvLibParser.Model_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#info_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitInfo_response(SvLibParser.Info_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#valuation_pair}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitValuation_pair(SvLibParser.Valuation_pairContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#t_valuation_pair}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitT_valuation_pair(SvLibParser.T_valuation_pairContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#check_sat_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCheck_sat_response(SvLibParser.Check_sat_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#echo_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitEcho_response(SvLibParser.Echo_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_assertions_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_assertions_response(SvLibParser.Get_assertions_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_assignment_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_assignment_response(SvLibParser.Get_assignment_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_info_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_info_response(SvLibParser.Get_info_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_model_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_model_response(SvLibParser.Get_model_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_option_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_option_response(SvLibParser.Get_option_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_proof_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_proof_response(SvLibParser.Get_proof_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_unsat_assump_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_unsat_assump_response(SvLibParser.Get_unsat_assump_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_unsat_core_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_unsat_core_response(SvLibParser.Get_unsat_core_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#get_value_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGet_value_response(SvLibParser.Get_value_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#specific_success_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSpecific_success_response(SvLibParser.Specific_success_responseContext ctx);

  /**
   * Visit a parse tree produced by {@link SvLibParser#general_response}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGeneral_response(SvLibParser.General_responseContext ctx);
}
