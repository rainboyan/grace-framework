/*
 * Copyright 2012-2023 the original author or authors.
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
package org.grails.plugins.web.rest.transform

import java.lang.reflect.Modifier

import jakarta.annotation.PostConstruct

import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import org.apache.groovy.ast.tools.AnnotatedNodeUtils
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.io.IOUtils
import grails.rest.Resource
import grails.rest.RestfulController
import grails.util.GrailsNameUtils
import grails.web.controllers.ControllerMethod
import grails.web.mapping.UrlMappings

import org.grails.compiler.injection.ArtefactTypeAstTransformation
import org.grails.compiler.injection.GrailsAwareInjectionOperation
import org.grails.compiler.injection.TraitInjectionUtils
import org.grails.compiler.web.ControllerActionTransformer
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.datastore.gorm.transactions.transform.TransactionalTransform

import static org.grails.compiler.injection.GrailsASTUtils.VOID_CLASS_NODE
import static org.grails.compiler.injection.GrailsASTUtils.ZERO_PARAMETERS
import static org.grails.compiler.injection.GrailsASTUtils.applyDefaultMethodTarget
import static org.grails.compiler.injection.GrailsASTUtils.buildThisExpression
import static org.grails.compiler.injection.GrailsASTUtils.nonGeneric
import static org.grails.compiler.injection.GrailsASTUtils.processVariableScopes

/**
 * The Resource transform automatically exposes a domain class as a RESTful resource.
 * In effect the transform adds a controller to a Grails application
 * that performs CRUD operations on the domain.
 * See the {@link Resource} annotation for more details
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class ResourceTransform implements ASTTransformation, CompilationUnitAware {

    private static final ClassNode MY_TYPE = new ClassNode(Resource)
    public static final String ATTR_READY_ONLY = 'readOnly'
    public static final String ATTR_SUPER_CLASS = 'superClass'
    public static final String RESPOND_METHOD = 'respond'
    public static final String ATTR_RESPONSE_FORMATS = 'formats'
    public static final String ATTR_URI = 'uri'
    public static final String PARAMS_VARIABLE = 'params'
    public static final ConstantExpression CONSTANT_STATUS = new ConstantExpression(ARGUMENT_STATUS)
    public static final String ATTR_NAMESPACE = 'namespace'
    public static final String RENDER_METHOD = 'render'
    public static final String ARGUMENT_STATUS = 'status'
    public static final String REDIRECT_METHOD = 'redirect'
    public static final ClassNode AUTOWIRED_CLASS_NODE = new ClassNode(Autowired).getPlainNodeReference()

    private CompilationUnit unit

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof ClassNode)) {
            throw new RuntimeException("Internal error: wrong types: ${astNodes[0].class} / ${astNodes[1].class}")
        }

        ClassNode parent = (ClassNode) astNodes[1]
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0]
        if (annotationNode.getClassNode() != MY_TYPE) {
            return
        }

        String className = "${parent.name}${ControllerArtefactHandler.TYPE}"
        File resource = IOUtils.findSourceFile(className)
        LinkableTransform.addLinkingMethods(parent)

        if (resource == null) {
            ClassNode superClassNode
            Expression superClassAttribute = annotationNode.getMember(ATTR_SUPER_CLASS)
            if (superClassAttribute instanceof ClassExpression) {
                superClassNode = ((ClassExpression) superClassAttribute).getType()
            }
            else {
                superClassNode = ClassHelper.make(RestfulController)
            }

            ModuleNode ast = source.getAST()
            ClassNode newControllerClassNode = new ClassNode(className, Modifier.PUBLIC, nonGeneric(superClassNode, parent))

            AnnotationNode transactionalAnn = new AnnotationNode(TransactionalTransform.MY_TYPE)
            transactionalAnn.addMember(ATTR_READY_ONLY, ConstantExpression.PRIM_TRUE)
            newControllerClassNode.addAnnotation(transactionalAnn)

            Expression readOnlyAttr = annotationNode.getMember(ATTR_READY_ONLY)
            boolean isReadOnly = readOnlyAttr != null && ((ConstantExpression) readOnlyAttr).trueExpression
            addConstructor(newControllerClassNode, parent, isReadOnly)

            List<ClassInjector> injectors = ArtefactTypeAstTransformation.findInjectors(ControllerArtefactHandler.TYPE,
                    GrailsAwareInjectionOperation.getClassInjectors())

            ArtefactTypeAstTransformation.performInjection(source, newControllerClassNode,
                    injectors.findAll { !(it instanceof ControllerActionTransformer) })

            if (unit) {
                TraitInjectionUtils.processTraitsForNode(source, newControllerClassNode, 'Controller', unit)
            }

            Expression responseFormatsAttr = annotationNode.getMember(ATTR_RESPONSE_FORMATS)
            Expression uriAttr = annotationNode.getMember(ATTR_URI)
            Expression namespaceAttr = annotationNode.getMember(ATTR_NAMESPACE)
            String domainPropertyName = GrailsNameUtils.getPropertyName(parent.getName())

            ListExpression responseFormatsExpression = new ListExpression()
            boolean hasHtml = false
            if (responseFormatsAttr != null) {
                if (responseFormatsAttr instanceof ConstantExpression) {
                    if (responseFormatsExpression.text.equalsIgnoreCase('html')) {
                        hasHtml = true
                    }
                    responseFormatsExpression.addExpression(responseFormatsAttr)
                }
                else if (responseFormatsAttr instanceof ListExpression) {
                    responseFormatsExpression = (ListExpression) responseFormatsAttr
                    for (Expression expr in responseFormatsExpression.expressions) {
                        if (expr.text.equalsIgnoreCase('html')) {
                            hasHtml = true
                            break
                        }
                    }
                }
            }
            else {
                responseFormatsExpression.addExpression(new ConstantExpression('json'))
                responseFormatsExpression.addExpression(new ConstantExpression('xml'))
            }

            if (uriAttr != null || namespaceAttr != null) {
                String uri = uriAttr?.getText()
                String namespace = namespaceAttr?.getText()
                if (uri || namespace) {
                    ClassNode urlMappingsClassNode = new ClassNode(UrlMappings).getPlainNodeReference()

                    FieldNode lazyInitField = new FieldNode('lazyInit', Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL,
                            ClassHelper.Boolean_TYPE, newControllerClassNode, new ConstantExpression(Boolean.FALSE))
                    newControllerClassNode.addField(lazyInitField)

                    FieldNode urlMappingsField = new FieldNode('$urlMappings', Modifier.PRIVATE,
                            urlMappingsClassNode, newControllerClassNode, null)
                    newControllerClassNode.addField(urlMappingsField)
                    Parameter urlMappingsSetterParam = new Parameter(urlMappingsClassNode, 'um')
                    AnnotationNode controllerMethodAnnotation = new AnnotationNode(new ClassNode(ControllerMethod).getPlainNodeReference())
                    MethodNode urlMappingsSetter = new MethodNode('setUrlMappings', Modifier.PUBLIC, VOID_CLASS_NODE,
                            [urlMappingsSetterParam] as Parameter[], null,
                            new ExpressionStatement(new BinaryExpression(new VariableExpression(urlMappingsField.name),
                                    Token.newSymbol(Types.EQUAL, 0, 0), new VariableExpression(urlMappingsSetterParam))))
                    AnnotationNode autowiredAnnotation = new AnnotationNode(AUTOWIRED_CLASS_NODE)
                    autowiredAnnotation.addMember('required', ConstantExpression.FALSE)

                    AnnotationNode qualifierAnnotation = new AnnotationNode(new ClassNode(Qualifier).getPlainNodeReference())
                    qualifierAnnotation.addMember('value', new ConstantExpression('grailsUrlMappingsHolder'))
                    urlMappingsSetter.addAnnotation(autowiredAnnotation)
                    urlMappingsSetter.addAnnotation(qualifierAnnotation)
                    urlMappingsSetter.addAnnotation(controllerMethodAnnotation)
                    newControllerClassNode.addMethod(urlMappingsSetter)
                    AnnotatedNodeUtils.markAsGenerated(newControllerClassNode, urlMappingsSetter)
                    processVariableScopes(source, newControllerClassNode, urlMappingsSetter)

                    BlockStatement methodBody = new BlockStatement()
                    VariableExpression urlMappingsVar = new VariableExpression(urlMappingsField.name)

                    MapExpression map = new MapExpression()
                    if (uri) {
                        map.addMapEntryExpression(new MapEntryExpression(new ConstantExpression('resources'),
                                new ConstantExpression(domainPropertyName)))
                    }

                    if (namespace) {
                        FieldNode namespaceField = new FieldNode('namespace', Modifier.STATIC, ClassHelper.STRING_TYPE,
                                newControllerClassNode, new ConstantExpression(namespace))
                        newControllerClassNode.addField(namespaceField)
                        if (map.getMapEntryExpressions().size() == 0) {
                            uri = "/${namespace}/${domainPropertyName}"
                            map.addMapEntryExpression(new MapEntryExpression(new ConstantExpression('resources'),
                                    new ConstantExpression(domainPropertyName)))
                        }
                        map.addMapEntryExpression(new MapEntryExpression(new ConstantExpression('namespace'),
                                new ConstantExpression(namespace)))
                    }

                    MethodCallExpression resourcesUrlMapping = new MethodCallExpression(buildThisExpression(), uri,
                            new MapExpression([ new MapEntryExpression(new ConstantExpression('resources'),
                                    new ConstantExpression(domainPropertyName))]))
                    ClosureExpression urlMappingsClosure = new ClosureExpression(null, new ExpressionStatement(resourcesUrlMapping))

                    MethodCallExpression addMappingsMethodCall = applyDefaultMethodTarget(new MethodCallExpression(
                            urlMappingsVar, 'addMappings', urlMappingsClosure), urlMappingsClassNode)
                    methodBody.addStatement(new IfStatement(new BooleanExpression(urlMappingsVar), new ExpressionStatement(addMappingsMethodCall),
                            new EmptyStatement()))

                    MethodNode initialiseUrlMappingsMethod = new MethodNode('initializeUrlMappings', Modifier.PUBLIC,
                            VOID_CLASS_NODE, ZERO_PARAMETERS, null, methodBody)
                    initialiseUrlMappingsMethod.addAnnotation(new AnnotationNode(new ClassNode(PostConstruct).getPlainNodeReference()))
                    initialiseUrlMappingsMethod.addAnnotation(controllerMethodAnnotation)
                    newControllerClassNode.addMethod(initialiseUrlMappingsMethod)
                    AnnotatedNodeUtils.markAsGenerated(newControllerClassNode, initialiseUrlMappingsMethod)
                    processVariableScopes(source, newControllerClassNode, initialiseUrlMappingsMethod)
                }
            }

            int publicStaticFinal = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL

            newControllerClassNode.addProperty('scope', publicStaticFinal, ClassHelper.STRING_TYPE,
                    new ConstantExpression('singleton'), null, null)
            newControllerClassNode.addProperty('responseFormats', publicStaticFinal,
                    new ClassNode(List).getPlainNodeReference(), responseFormatsExpression, null, null)

            ArtefactTypeAstTransformation.performInjection(source, newControllerClassNode,
                    injectors.findAll { it instanceof ControllerActionTransformer })
            new TransactionalTransform().visit(source, transactionalAnn, newControllerClassNode)
            newControllerClassNode.setModule(ast)

            AnnotationNode artefactAnnotation = new AnnotationNode(new ClassNode(Artefact))
            artefactAnnotation.addMember('value', new ConstantExpression(ControllerArtefactHandler.TYPE))
            newControllerClassNode.addAnnotation(artefactAnnotation)

            ast.classes.add(newControllerClassNode)
        }
    }

    ConstructorNode addConstructor(ClassNode controllerClassNode, ClassNode domainClassNode, boolean readOnly) {
        BlockStatement constructorBody = new BlockStatement()
        constructorBody.addStatement(new ExpressionStatement(new ConstructorCallExpression(ClassNode.SUPER,
                new TupleExpression(new ClassExpression(domainClassNode), new ConstantExpression(readOnly, true)))))
        ConstructorNode constructorNode = controllerClassNode.addConstructor(Modifier.PUBLIC, ZERO_PARAMETERS,
                ClassNode.EMPTY_ARRAY, constructorBody)
        AnnotatedNodeUtils.markAsGenerated(controllerClassNode, constructorNode)
        constructorNode
    }

    @Override
    void setCompilationUnit(CompilationUnit unit) {
        this.unit = unit
    }

}
