// Generated from AcslGrammar.g4 by ANTLR 4.7.1
package org.sosy_lab.cpachecker.cfa.ast.acsl.generated;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link AcslGrammarParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface AcslGrammarVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId(AcslGrammarParser.IdContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(AcslGrammarParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TrueConstant}
	 * labeled alternative in {@link AcslGrammarParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrueConstant(AcslGrammarParser.TrueConstantContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FalseConstant}
	 * labeled alternative in {@link AcslGrammarParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFalseConstant(AcslGrammarParser.FalseConstantContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CConstant}
	 * labeled alternative in {@link AcslGrammarParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCConstant(AcslGrammarParser.CConstantContext ctx);
	/**
	 * Visit a parse tree produced by the {@code StringConstant}
	 * labeled alternative in {@link AcslGrammarParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringConstant(AcslGrammarParser.StringConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#binOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinOp(AcslGrammarParser.BinOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#unaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOp(AcslGrammarParser.UnaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#relationalTermOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalTermOp(AcslGrammarParser.RelationalTermOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BlockLengthTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockLengthTerm(AcslGrammarParser.BlockLengthTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OffsetTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffsetTerm(AcslGrammarParser.OffsetTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NullTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullTerm(AcslGrammarParser.NullTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TernaryCondTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryCondTerm(AcslGrammarParser.TernaryCondTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AllocationTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllocationTerm(AcslGrammarParser.AllocationTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OldTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOldTerm(AcslGrammarParser.OldTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SyntacticNamingTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSyntacticNamingTerm(AcslGrammarParser.SyntacticNamingTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExitStatusTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExitStatusTerm(AcslGrammarParser.ExitStatusTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralTerm(AcslGrammarParser.LiteralTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CastTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCastTerm(AcslGrammarParser.CastTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BinaryOpTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryOpTerm(AcslGrammarParser.BinaryOpTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ArrayFuncModifierTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayFuncModifierTerm(AcslGrammarParser.ArrayFuncModifierTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PointerStructureFieldAccessTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointerStructureFieldAccessTerm(AcslGrammarParser.PointerStructureFieldAccessTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AtTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtTerm(AcslGrammarParser.AtTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SizeofTypeTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSizeofTypeTerm(AcslGrammarParser.SizeofTypeTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BaseAddrTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseAddrTerm(AcslGrammarParser.BaseAddrTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocalBindingTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalBindingTerm(AcslGrammarParser.LocalBindingTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SizeofTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSizeofTerm(AcslGrammarParser.SizeofTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code StructureFieldAccessTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructureFieldAccessTerm(AcslGrammarParser.StructureFieldAccessTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ArrayAccessTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayAccessTerm(AcslGrammarParser.ArrayAccessTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ResultTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResultTerm(AcslGrammarParser.ResultTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryOpTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOpTerm(AcslGrammarParser.UnaryOpTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code VariableTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableTerm(AcslGrammarParser.VariableTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FuncApplicationTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncApplicationTerm(AcslGrammarParser.FuncApplicationTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FieldFuncModifierTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldFuncModifierTerm(AcslGrammarParser.FieldFuncModifierTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParenthesesTerm}
	 * labeled alternative in {@link AcslGrammarParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesesTerm(AcslGrammarParser.ParenthesesTermContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#poly_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPoly_id(AcslGrammarParser.Poly_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#relOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelOp(AcslGrammarParser.RelOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#binaryPredOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryPredOp(AcslGrammarParser.BinaryPredOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParenthesesPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesesPred(AcslGrammarParser.ParenthesesPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FreshPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFreshPred(AcslGrammarParser.FreshPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TernaryConditionTermPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryConditionTermPred(AcslGrammarParser.TernaryConditionTermPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TernaryConditionPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryConditionPred(AcslGrammarParser.TernaryConditionPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PredicateApplicationPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicateApplicationPred(AcslGrammarParser.PredicateApplicationPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LogicalFalsePred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalFalsePred(AcslGrammarParser.LogicalFalsePredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AllocablePred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllocablePred(AcslGrammarParser.AllocablePredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UniversalQuantificationPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUniversalQuantificationPred(AcslGrammarParser.UniversalQuantificationPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SyntacticNamingPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSyntacticNamingPred(AcslGrammarParser.SyntacticNamingPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ComparisonPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonPred(AcslGrammarParser.ComparisonPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NegationPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegationPred(AcslGrammarParser.NegationPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExistentialQuantificationPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExistentialQuantificationPred(AcslGrammarParser.ExistentialQuantificationPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code oldPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOldPred(AcslGrammarParser.OldPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PredicateVariable}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicateVariable(AcslGrammarParser.PredicateVariableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocalBindingPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalBindingPred(AcslGrammarParser.LocalBindingPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SetInclusionPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetInclusionPred(AcslGrammarParser.SetInclusionPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SeparatedPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeparatedPred(AcslGrammarParser.SeparatedPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LogicalTruePred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalTruePred(AcslGrammarParser.LogicalTruePredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ValidPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValidPred(AcslGrammarParser.ValidPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BinaryPredicate}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryPredicate(AcslGrammarParser.BinaryPredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SetMembershipPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetMembershipPred(AcslGrammarParser.SetMembershipPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code InitializedPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializedPred(AcslGrammarParser.InitializedPredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FreeablePred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFreeablePred(AcslGrammarParser.FreeablePredContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ValidReadPred}
	 * labeled alternative in {@link AcslGrammarParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValidReadPred(AcslGrammarParser.ValidReadPredContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#ident}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdent(AcslGrammarParser.IdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#binders}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinders(AcslGrammarParser.BindersContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#binder}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinder(AcslGrammarParser.BinderContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#type_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_expr(AcslGrammarParser.Type_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#logic_type_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogic_type_expr(AcslGrammarParser.Logic_type_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#built_in_logic_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBuilt_in_logic_type(AcslGrammarParser.Built_in_logic_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#variable_ident}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_ident(AcslGrammarParser.Variable_identContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#function_contract}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_contract(AcslGrammarParser.Function_contractContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#requires_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRequires_clause(AcslGrammarParser.Requires_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#terminates_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTerminates_clause(AcslGrammarParser.Terminates_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#decreases_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecreases_clause(AcslGrammarParser.Decreases_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#simple_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_clause(AcslGrammarParser.Simple_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#assigns_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssigns_clause(AcslGrammarParser.Assigns_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#strings}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStrings(AcslGrammarParser.StringsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#locations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocations(AcslGrammarParser.LocationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#location}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocation(AcslGrammarParser.LocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#ensures_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnsures_clause(AcslGrammarParser.Ensures_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#named_behavior}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamed_behavior(AcslGrammarParser.Named_behaviorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#behavior_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBehavior_body(AcslGrammarParser.Behavior_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#assumes_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssumes_clause(AcslGrammarParser.Assumes_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#completeness_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompleteness_clause(AcslGrammarParser.Completeness_clauseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetIntersection}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetIntersection(AcslGrammarParser.TsetIntersectionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetParen}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetParen(AcslGrammarParser.TsetParenContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetUnion}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetUnion(AcslGrammarParser.TsetUnionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetPointerAccess}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetPointerAccess(AcslGrammarParser.TsetPointerAccessContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetTerm}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetTerm(AcslGrammarParser.TsetTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetBinders}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetBinders(AcslGrammarParser.TsetBindersContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetArrayAccess}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetArrayAccess(AcslGrammarParser.TsetArrayAccessContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetDeref}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetDeref(AcslGrammarParser.TsetDerefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetAddr}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetAddr(AcslGrammarParser.TsetAddrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetPlus}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetPlus(AcslGrammarParser.TsetPlusContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetSet}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetSet(AcslGrammarParser.TsetSetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetRange}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetRange(AcslGrammarParser.TsetRangeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetMemberAccess}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetMemberAccess(AcslGrammarParser.TsetMemberAccessContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TsetEmpty}
	 * labeled alternative in {@link AcslGrammarParser#tset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsetEmpty(AcslGrammarParser.TsetEmptyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#c_compound_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_compound_statement(AcslGrammarParser.C_compound_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#c_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_statement(AcslGrammarParser.C_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#assertion}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssertion(AcslGrammarParser.AssertionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AllocatesClause}
	 * labeled alternative in {@link AcslGrammarParser#allocation_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllocatesClause(AcslGrammarParser.AllocatesClauseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FreesClause}
	 * labeled alternative in {@link AcslGrammarParser#allocation_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFreesClause(AcslGrammarParser.FreesClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#loop_allocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_allocation(AcslGrammarParser.Loop_allocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#dyn_allocation_addresses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDyn_allocation_addresses(AcslGrammarParser.Dyn_allocation_addressesContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#one_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOne_label(AcslGrammarParser.One_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#two_labels}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTwo_labels(AcslGrammarParser.Two_labelsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#location_addresses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocation_addresses(AcslGrammarParser.Location_addressesContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#location_address}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocation_address(AcslGrammarParser.Location_addressContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#abrupt_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbrupt_clause(AcslGrammarParser.Abrupt_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#exits_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExits_clause(AcslGrammarParser.Exits_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#abrupt_clause_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbrupt_clause_stmt(AcslGrammarParser.Abrupt_clause_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#breaks_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreaks_clause(AcslGrammarParser.Breaks_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#continues_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinues_clause(AcslGrammarParser.Continues_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#returns_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturns_clause(AcslGrammarParser.Returns_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#label_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel_id(AcslGrammarParser.Label_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#loop_annot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_annot(AcslGrammarParser.Loop_annotContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#loop_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_clause(AcslGrammarParser.Loop_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#loop_invariant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_invariant(AcslGrammarParser.Loop_invariantContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#loop_assigns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_assigns(AcslGrammarParser.Loop_assignsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#loop_behavior}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_behavior(AcslGrammarParser.Loop_behaviorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#loop_variant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_variant(AcslGrammarParser.Loop_variantContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#statement_contract}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement_contract(AcslGrammarParser.Statement_contractContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#simple_clause_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_clause_stmt(AcslGrammarParser.Simple_clause_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#named_behavior_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamed_behavior_stmt(AcslGrammarParser.Named_behavior_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#behavior_body_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBehavior_body_stmt(AcslGrammarParser.Behavior_body_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(AcslGrammarParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#genericSelection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericSelection(AcslGrammarParser.GenericSelectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#genericAssocList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericAssocList(AcslGrammarParser.GenericAssocListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#genericAssociation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericAssociation(AcslGrammarParser.GenericAssociationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpression(AcslGrammarParser.PostfixExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#argumentExpressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentExpressionList(AcslGrammarParser.ArgumentExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpression(AcslGrammarParser.UnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#unaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOperator(AcslGrammarParser.UnaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#castExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCastExpression(AcslGrammarParser.CastExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(AcslGrammarParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(AcslGrammarParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#shiftExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShiftExpression(AcslGrammarParser.ShiftExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpression(AcslGrammarParser.RelationalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#equalityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpression(AcslGrammarParser.EqualityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#andExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpression(AcslGrammarParser.AndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#exclusiveOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExclusiveOrExpression(AcslGrammarParser.ExclusiveOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#inclusiveOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclusiveOrExpression(AcslGrammarParser.InclusiveOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpression(AcslGrammarParser.LogicalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpression(AcslGrammarParser.LogicalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#conditionalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionalExpression(AcslGrammarParser.ConditionalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#assignmentExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentExpression(AcslGrammarParser.AssignmentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#assignmentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperator(AcslGrammarParser.AssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(AcslGrammarParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#constantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantExpression(AcslGrammarParser.ConstantExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(AcslGrammarParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#declarationSpecifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationSpecifiers(AcslGrammarParser.DeclarationSpecifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#declarationSpecifiers2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationSpecifiers2(AcslGrammarParser.DeclarationSpecifiers2Context ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#declarationSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationSpecifier(AcslGrammarParser.DeclarationSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#initDeclaratorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitDeclaratorList(AcslGrammarParser.InitDeclaratorListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#initDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitDeclarator(AcslGrammarParser.InitDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#storageClassSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorageClassSpecifier(AcslGrammarParser.StorageClassSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#typeSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeSpecifier(AcslGrammarParser.TypeSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#structOrUnionSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructOrUnionSpecifier(AcslGrammarParser.StructOrUnionSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#structOrUnion}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructOrUnion(AcslGrammarParser.StructOrUnionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#structDeclarationList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDeclarationList(AcslGrammarParser.StructDeclarationListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#structDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDeclaration(AcslGrammarParser.StructDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#specifierQualifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecifierQualifierList(AcslGrammarParser.SpecifierQualifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#structDeclaratorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDeclaratorList(AcslGrammarParser.StructDeclaratorListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#structDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDeclarator(AcslGrammarParser.StructDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#enumSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumSpecifier(AcslGrammarParser.EnumSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#enumeratorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumeratorList(AcslGrammarParser.EnumeratorListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#enumerator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumerator(AcslGrammarParser.EnumeratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#enumerationConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumerationConstant(AcslGrammarParser.EnumerationConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#atomicTypeSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomicTypeSpecifier(AcslGrammarParser.AtomicTypeSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#typeQualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeQualifier(AcslGrammarParser.TypeQualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#functionSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionSpecifier(AcslGrammarParser.FunctionSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#alignmentSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlignmentSpecifier(AcslGrammarParser.AlignmentSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarator(AcslGrammarParser.DeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#directDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectDeclarator(AcslGrammarParser.DirectDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#vcSpecificModifer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVcSpecificModifer(AcslGrammarParser.VcSpecificModiferContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#gccDeclaratorExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGccDeclaratorExtension(AcslGrammarParser.GccDeclaratorExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#gccAttributeSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGccAttributeSpecifier(AcslGrammarParser.GccAttributeSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#gccAttributeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGccAttributeList(AcslGrammarParser.GccAttributeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#gccAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGccAttribute(AcslGrammarParser.GccAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#nestedParenthesesBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestedParenthesesBlock(AcslGrammarParser.NestedParenthesesBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#pointer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer(AcslGrammarParser.PointerContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#typeQualifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeQualifierList(AcslGrammarParser.TypeQualifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#parameterTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterTypeList(AcslGrammarParser.ParameterTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#parameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterList(AcslGrammarParser.ParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#parameterDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterDeclaration(AcslGrammarParser.ParameterDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierList(AcslGrammarParser.IdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(AcslGrammarParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#abstractDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbstractDeclarator(AcslGrammarParser.AbstractDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#directAbstractDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectAbstractDeclarator(AcslGrammarParser.DirectAbstractDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#typedefName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypedefName(AcslGrammarParser.TypedefNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializer(AcslGrammarParser.InitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#initializerList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializerList(AcslGrammarParser.InitializerListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#designation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignation(AcslGrammarParser.DesignationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#designatorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignatorList(AcslGrammarParser.DesignatorListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#designator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignator(AcslGrammarParser.DesignatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#staticAssertDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStaticAssertDeclaration(AcslGrammarParser.StaticAssertDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(AcslGrammarParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#labeledStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabeledStatement(AcslGrammarParser.LabeledStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#compoundStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompoundStatement(AcslGrammarParser.CompoundStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#blockItemList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockItemList(AcslGrammarParser.BlockItemListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#blockItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockItem(AcslGrammarParser.BlockItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(AcslGrammarParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#selectionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectionStatement(AcslGrammarParser.SelectionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#iterationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIterationStatement(AcslGrammarParser.IterationStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#forCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForCondition(AcslGrammarParser.ForConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#forDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForDeclaration(AcslGrammarParser.ForDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#forExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForExpression(AcslGrammarParser.ForExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#jumpStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJumpStatement(AcslGrammarParser.JumpStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(AcslGrammarParser.CompilationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#translationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTranslationUnit(AcslGrammarParser.TranslationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#externalDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternalDeclaration(AcslGrammarParser.ExternalDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#functionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDefinition(AcslGrammarParser.FunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AcslGrammarParser#declarationList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationList(AcslGrammarParser.DeclarationListContext ctx);
}