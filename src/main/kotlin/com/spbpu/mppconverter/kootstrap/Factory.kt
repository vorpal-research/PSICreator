package com.spbpu.mppconverter.kootstrap

import org.jetbrains.kotlin.psi.KtPsiFactory

object Factory {
    val file = PSICreator().getPSIForFile("test/HelloWorld.kt")
    val psiFactory = KtPsiFactory(file.project)
}