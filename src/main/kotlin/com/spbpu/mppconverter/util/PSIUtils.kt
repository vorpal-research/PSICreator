package com.spbpu.mppconverter.util

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import java.util.ArrayList

fun PsiElement.getAllChildrenOfCurLevel(): List<PsiElement> = this.node.getAllChildrenOfCurLevel().map { it.psi }

fun List<ASTNode>.getAllChildren(): List<ASTNode> {
    if (this.isEmpty()) return listOf()
    val res = arrayListOf<ASTNode>()
    for (node in this) {
        node.getAllChildrenOfCurLevel().forEach { res.add(it) }
    }
    return res
}

fun ASTNode.getAllChildrenOfCurLevel(): Array<ASTNode> = this.getChildren(TokenSet.ANY)

fun ASTNode.getAllChildrenOfTheLevel(level: Int): List<ASTNode> {
    var res = this.getAllChildrenOfCurLevel().toList()
    for (i in 1 until level)
        res = res.getAllChildren()
    return res
}

fun ASTNode.getAllChildrenNodes(): ArrayList<ASTNode> {
    val result = ArrayList<ASTNode>()
    var level = 1
    var children = this.getAllChildrenOfCurLevel()
    while (children.isNotEmpty()) {
        children.forEach { result.add(it) }
        ++level
        children = this.getAllChildrenOfTheLevel(level).toTypedArray()
    }
    return result
}

fun PsiElement.getAllChildren(): List<PsiElement> = this.node.getAllChildrenNodes().map { it.psi }


fun PsiElement.debugPrint() {
    println("---BEGIN PSI STRUCTURE---")
    debugPrint(0)
    println("---END PSI STRUCTURE---")
}

fun PsiElement.debugPrint(indentation: Int) {
    println("|".repeat(indentation) + toString())
    for (child in children)
        child.debugPrint(indentation + 1)
    if (children.isEmpty())
        println("|".repeat(indentation + 1) + "'$text'")
}
