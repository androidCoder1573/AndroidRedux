package com.cyworks.redux.compiler

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

class ReactUIDataPlugin : SymbolProcessor {
//    override fun process(resolver: Resolver): List<KSAnnotatedElement> {
//        context.elementsAnnotatedWith<Delegate>().forEach { element ->
//            if (element.kind != ElementKind.FIELD) {
//                context.error("Only fields can be annotated with @Delegate", element)
//                return@forEach
//            }
//
//            val delegateClass = element.getAnnotation(Delegate::class.java).delegateClass
//            val delegateName = delegateClass.simpleName
//            val fieldName = element.simpleName.toString()
//
//            val delegateType = delegateClass.asClassName().parameterizedBy(STAR)
//            val fieldType = delegateType.copy(nullable = true)
//
//            val delegateProperty = PropertySpec.builder(fieldName, delegateType)
//                .initializer("%T()", delegateClass)
//                .addModifiers(KModifier.PRIVATE)
//                .build()
//
//            val getter = FunSpec.getterBuilder()
//                .addStatement("return $fieldName")
//                .build()
//
//            val fieldProperty = PropertySpec.builder(fieldName, fieldType)
//                .getter(getter)
//                .build()
//
//            val file = FileSpec.builder(delegateClass.packageName, delegateName)
//                .addType(TypeSpec.classBuilder(delegateName)
//                    .addProperty(delegateProperty)
//                    .build())
//                .build()
//
//            context.addGeneratedFile(file)
//            context.addGeneratedProperty(fieldProperty)
//        }

    //        val delegateAnnotation = resolver.getClassDeclarationByName("Delegate") ?: return emptyList()
//
//        val delegateProperties = mutableListOf<KSProperty>()
//
//        resolver.getSymbolsWithAnnotation(delegateAnnotation).forEach { symbol ->
//            if (symbol !is KSProperty) {
//                resolver.report("Only properties can be annotated with @Delegate", symbol)
//                return@forEach
//            }
//
//            delegateProperties.add(symbol)
//        }
//
//        delegateProperties.forEach { property ->
//            // 处理委托属性的逻辑
//            // 生成委托代码
//        }
//
//        return emptyList()
//    }
    override fun process(resolver: Resolver): List<KSAnnotated> {
        return emptyList()
    }
}