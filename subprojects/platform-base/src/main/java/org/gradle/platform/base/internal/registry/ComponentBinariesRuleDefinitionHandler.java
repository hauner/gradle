/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.platform.base.internal.registry;

import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.model.InvalidModelRuleDeclarationException;
import org.gradle.model.collection.CollectionBuilder;
import org.gradle.model.collection.internal.DefaultCollectionBuilder;
import org.gradle.model.entity.internal.NamedEntityInstantiator;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.core.rule.describe.SimpleModelRuleDescriptor;
import org.gradle.model.internal.inspect.MethodRuleDefinition;
import org.gradle.model.internal.inspect.RuleSourceDependencies;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.platform.base.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ComponentBinariesRuleDefinitionHandler extends AbstractAnnotationDrivenMethodComponentRuleDefinitionHandler<ComponentBinaries> {

    public <R> void register(final MethodRuleDefinition<R> ruleDefinition, final ModelRegistry modelRegistry, RuleSourceDependencies dependencies) {
        doRegister(ruleDefinition, modelRegistry, dependencies);
    }

    private <R, S extends BinarySpec> void doRegister(MethodRuleDefinition<R> ruleDefinition, ModelRegistry modelRegistry, RuleSourceDependencies dependencies) {
        try {
            RuleMethodDataCollector dataCollector = new RuleMethodDataCollector();
            visitAndVerifyMethodSignature(dataCollector, ruleDefinition);

            final Class<S> binaryType = dataCollector.getParameterType(BinarySpec.class);
            final Class<? extends ComponentSpec<S>> componentType = dataCollector.getParameterType(ComponentSpec.class);

            validateComponentType(binaryType, componentType);

            dependencies.add(ComponentModelBasePlugin.class);
            final ModelReference<BinaryContainer> subject = ModelReference.of(ModelPath.path("binaries"), new ModelType<BinaryContainer>() {
            });

            configureMutationRule(modelRegistry, subject, componentType, binaryType, ruleDefinition);
        } catch (InvalidComponentModelException e) {
            invalidModelRule(ruleDefinition, e);
        }
    }

    private <S extends BinarySpec, R> void configureMutationRule(ModelRegistry modelRegistry, ModelReference<BinaryContainer> subject, Class<? extends ComponentSpec<S>> componentType, Class<S> binaryType, MethodRuleDefinition<R> ruleDefinition) {
        modelRegistry.mutate(new ComponentBinariesRule<R, S>(subject, componentType, binaryType, ruleDefinition, modelRegistry));
    }

    private <R> void visitAndVerifyMethodSignature(RuleMethodDataCollector dataCollector, MethodRuleDefinition<R> ruleDefinition) {
        assertIsVoidMethod(ruleDefinition);
        visitCollectionBuilderSubject(dataCollector, ruleDefinition, BinarySpec.class);
        visitDependency(dataCollector, ruleDefinition, ModelType.of(ComponentSpec.class));
    }

    private <T extends BinarySpec> void validateComponentType(Class<T> expectedBinaryType, Class<? extends ComponentSpec<T>> componentType) {
        if (componentType == null) {
            throw new InvalidComponentModelException(String.format("%s method must have one parameter extending %s. Found no parameter extending %s.", annotationType.getSimpleName(),
                    ComponentSpec.class.getSimpleName(),
                    ComponentSpec.class.getSimpleName()));
        }

        for (Type type : componentType.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                if (ComponentSpec.class.isAssignableFrom((Class<?>)parameterizedType.getRawType())) {
                    for (Type givenBinaryType : parameterizedType.getActualTypeArguments()) {
                        if (((Class<?>) givenBinaryType).isAssignableFrom(expectedBinaryType)) {
                            return;
                        }
                    }
                }
            }
        }
        throw new InvalidComponentModelException(String.format("%s method parameter of type %s does not support binaries of type %s.", annotationType.getSimpleName(),
                componentType.getSimpleName(),
                expectedBinaryType.getSimpleName()));

    }

    private class ComponentBinariesRule<R, S extends BinarySpec> extends CollectionBuilderBasedRule<R, S, ComponentSpec<S>, BinaryContainer> {

        private final Class<? extends ComponentSpec<S>> componentType;
        private final ModelRegistry modelRegistry;
        private final Class<S> binaryType;

        public ComponentBinariesRule(ModelReference<BinaryContainer> subject, final Class<? extends ComponentSpec<S>> componentType, final Class<S> binaryType, MethodRuleDefinition<R> ruleDefinition, ModelRegistry modelRegistry) {
            super(subject, componentType, ruleDefinition, ModelReference.of("componentSpecs", ComponentSpecContainer.class));
            this.componentType = componentType;
            this.binaryType = binaryType;
            this.modelRegistry = modelRegistry;
        }

        public void mutate(final BinaryContainer binaries, final Inputs inputs) {
            ComponentSpecContainer componentSpecs = inputs.get(0, ModelType.of(ComponentSpecContainer.class)).getInstance();

            for (final ComponentSpec<S> componentSpec : componentSpecs.withType(componentType)) {
                NamedEntityInstantiator<S> namedEntityInstantiator = new Instantiator<S>(binaryType, componentSpec, binaries);
                CollectionBuilder<S> collectionBuilder = new DefaultCollectionBuilder<S>(
                        getSubject().getPath(),
                        namedEntityInstantiator,
                        new SimpleModelRuleDescriptor("Project.<init>.binaries()"),
                        inputs,
                        modelRegistry);
                invoke(inputs, collectionBuilder, componentSpec, componentSpecs);
            }
        }
    }

    protected <R> void invalidModelRule(MethodRuleDefinition<R> ruleDefinition, InvalidComponentModelException e) {
        StringBuilder sb = new StringBuilder();
        ruleDefinition.getDescriptor().describeTo(sb);
        sb.append(" is not a valid ComponentBinaries model rule method.");
        throw new InvalidModelRuleDeclarationException(sb.toString(), e);
    }

    private class Instantiator<S extends BinarySpec> implements NamedEntityInstantiator<S> {
        private Class<S> binaryType;
        private final ComponentSpec<S> componentSpec;
        private final BinaryContainer container;

        public Instantiator(Class<S> binaryType, ComponentSpec<S> componentSpec, BinaryContainer container) {
            this.binaryType = binaryType;
            this.componentSpec = componentSpec;
            this.container = container;
        }

        public ModelType<S> getType() {
            return ModelType.of(binaryType);
        }

        public S create(String name) {
            S binary = container.create(name, binaryType);
            componentSpec.getBinaries().add(binary);
            return binary;
        }

        public <U extends S> U create(String name, Class<U> type) {
            U binary = container.create(name, type);
            componentSpec.getBinaries().add(binary);
            return binary;
        }
    }
}
