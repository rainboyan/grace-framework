/*
 * Copyright 2011-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.compiler.injection;

import java.lang.reflect.Modifier;

import org.apache.groovy.ast.tools.AnnotatedNodeUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.springframework.validation.Errors;

import grails.compiler.ast.GrailsArtefactClassInjector;
import grails.validation.ValidationErrors;

/**
 * @author Jeff Brown
 */
public class ASTValidationErrorsHelper implements ASTErrorsHelper {

    private static final ConstantExpression NULL_EXPRESSION = new ConstantExpression(null);

    private static final String SET_ERRORS_METHOD_NAME = "setErrors";

    private static final String GET_ERRORS_METHOD_NAME = "getErrors";

    private static final String HAS_ERRORS_METHOD_NAME = "hasErrors";

    private static final String CLEAR_ERRORS_METHOD_NAME = "clearErrors";

    private static final String INIT_ERRORS_METHOD_NAME = "initErrors";

    private static final String ERRORS_PROPERTY_NAME = "errors";

    private static final Token EQUALS_SYMBOL = Token.newSymbol(Types.EQUALS, 0, 0);

    private static final ClassNode ERRORS_CLASS_NODE = new ClassNode(Errors.class);

    private static final VariableExpression ERRORS_EXPRESSION = new VariableExpression(ERRORS_PROPERTY_NAME);

    private static final TupleExpression EMPTY_TUPLE = new TupleExpression();

    public void injectErrorsCode(ClassNode classNode) {
        addErrorsField(classNode);
        addInitErrorsMethod(classNode);
        addGetErrorsMethod(classNode);
        addHasErrorsMethod(classNode);
        addSetErrorsMethod(classNode);
        addClearErrorsMethod(classNode);
    }

    protected void addErrorsField(final ClassNode paramTypeClassNode) {
        ASTNode errorsField = paramTypeClassNode.getField(ERRORS_PROPERTY_NAME);
        if (errorsField == null) {
            paramTypeClassNode.addField(new FieldNode(ERRORS_PROPERTY_NAME, Modifier.PRIVATE,
                    ERRORS_CLASS_NODE, paramTypeClassNode, NULL_EXPRESSION));
        }
    }

    protected void addInitErrorsMethod(final ClassNode paramTypeClassNode) {
        ASTNode initErrorsMethod = paramTypeClassNode.getMethod(INIT_ERRORS_METHOD_NAME, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if (initErrorsMethod == null) {
            BlockStatement initErrorsMethodCode = new BlockStatement();

            BinaryExpression errorsIsNullExpression = new BinaryExpression(ERRORS_EXPRESSION, Token.newSymbol(
                    Types.COMPARE_EQUAL, 0, 0), NULL_EXPRESSION);

            Expression beanPropertyBindingResultConstructorArgs = new ArgumentListExpression(
                    new VariableExpression("this"), new ConstantExpression(paramTypeClassNode.getName()));
            Statement newEvaluatorExpression = new ExpressionStatement(
                    new BinaryExpression(ERRORS_EXPRESSION,
                            EQUALS_SYMBOL,
                            new ConstructorCallExpression(new ClassNode(
                                    ValidationErrors.class),
                                    beanPropertyBindingResultConstructorArgs)));
            Statement initErrorsIfNullStatement = new IfStatement(
                    new BooleanExpression(errorsIsNullExpression), newEvaluatorExpression,
                    new ExpressionStatement(new EmptyExpression()));
            initErrorsMethodCode.addStatement(initErrorsIfNullStatement);

            MethodNode methodNode = new MethodNode(INIT_ERRORS_METHOD_NAME,
                    Modifier.PRIVATE, ClassHelper.VOID_TYPE,
                    GrailsArtefactClassInjector.ZERO_PARAMETERS, GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, initErrorsMethodCode);
            paramTypeClassNode.addMethod(methodNode);
            AnnotatedNodeUtils.markAsGenerated(paramTypeClassNode, methodNode);
        }
    }

    protected void addClearErrorsMethod(ClassNode paramTypeClassNode) {
        ASTNode clearErrorsMethod = paramTypeClassNode.getMethod(CLEAR_ERRORS_METHOD_NAME, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if (clearErrorsMethod == null) {
            BlockStatement clearErrorsMethodCode = new BlockStatement();
            Expression nullOutErrorsFieldExpression = new BinaryExpression(ERRORS_EXPRESSION,
                    EQUALS_SYMBOL, NULL_EXPRESSION);
            clearErrorsMethodCode.addStatement(new ExpressionStatement(nullOutErrorsFieldExpression));

            MethodNode methodNode = new MethodNode(CLEAR_ERRORS_METHOD_NAME,
                    Modifier.PUBLIC, ClassHelper.VOID_TYPE,
                    GrailsArtefactClassInjector.ZERO_PARAMETERS, GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, clearErrorsMethodCode);
            paramTypeClassNode.addMethod(methodNode);
            AnnotatedNodeUtils.markAsGenerated(paramTypeClassNode, methodNode);
        }
    }

    protected void addHasErrorsMethod(ClassNode paramTypeClassNode) {
        ASTNode getErrorsMethod = paramTypeClassNode.getMethod(HAS_ERRORS_METHOD_NAME, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if (getErrorsMethod == null) {
            BlockStatement hasErrorsMethodCode = new BlockStatement();
            Expression initErrorsMethodCallExpression = new MethodCallExpression(new VariableExpression("this"),
                    INIT_ERRORS_METHOD_NAME, EMPTY_TUPLE);
            hasErrorsMethodCode.addStatement(new ExpressionStatement(initErrorsMethodCallExpression));
            Statement returnStatement = new ReturnStatement(new BooleanExpression(new MethodCallExpression(ERRORS_EXPRESSION,
                    HAS_ERRORS_METHOD_NAME, EMPTY_TUPLE)));
            hasErrorsMethodCode.addStatement(returnStatement);

            MethodNode methodNode = new MethodNode(HAS_ERRORS_METHOD_NAME,
                    Modifier.PUBLIC, new ClassNode(Boolean.class),
                    GrailsArtefactClassInjector.ZERO_PARAMETERS, GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, hasErrorsMethodCode);
            paramTypeClassNode.addMethod(methodNode);
            AnnotatedNodeUtils.markAsGenerated(paramTypeClassNode, methodNode);
        }
    }

    protected void addGetErrorsMethod(ClassNode paramTypeClassNode) {
        ASTNode getErrorsMethod = paramTypeClassNode.getMethod(GET_ERRORS_METHOD_NAME, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if (getErrorsMethod == null) {
            BlockStatement getErrorsMethodCode = new BlockStatement();
            Expression initErrorsMethodCallExpression = new MethodCallExpression(new VariableExpression("this"),
                    INIT_ERRORS_METHOD_NAME, EMPTY_TUPLE);
            getErrorsMethodCode.addStatement(new ExpressionStatement(initErrorsMethodCallExpression));
            Statement returnStatement = new ReturnStatement(ERRORS_EXPRESSION);
            getErrorsMethodCode.addStatement(returnStatement);

            MethodNode methodNode = new MethodNode(GET_ERRORS_METHOD_NAME,
                    Modifier.PUBLIC, ERRORS_CLASS_NODE,
                    GrailsArtefactClassInjector.ZERO_PARAMETERS, GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, getErrorsMethodCode);
            paramTypeClassNode.addMethod(methodNode);
            AnnotatedNodeUtils.markAsGenerated(paramTypeClassNode, methodNode);
        }
    }

    protected void addSetErrorsMethod(ClassNode paramTypeClassNode) {
        String errorsArgumentName = "$errorsArg";
        MethodNode setErrorsMethod = paramTypeClassNode.getMethod(SET_ERRORS_METHOD_NAME,
                new Parameter[] { new Parameter(ERRORS_CLASS_NODE, errorsArgumentName) });
        if (setErrorsMethod == null) {
            Expression assignErrorsExpression = new BinaryExpression(ERRORS_EXPRESSION,
                    EQUALS_SYMBOL, new VariableExpression(errorsArgumentName));
            setErrorsMethod = new MethodNode(SET_ERRORS_METHOD_NAME,
                    Modifier.PUBLIC, ClassHelper.VOID_TYPE,
                    new Parameter[] { new Parameter(ERRORS_CLASS_NODE, errorsArgumentName) }, GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY,
                    new ExpressionStatement(assignErrorsExpression));
            paramTypeClassNode.addMethod(setErrorsMethod);
            AnnotatedNodeUtils.markAsGenerated(paramTypeClassNode, setErrorsMethod);
        }
    }

}
