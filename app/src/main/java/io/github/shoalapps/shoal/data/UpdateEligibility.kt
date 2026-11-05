package io.github.shoalapps.shoal.data

/** Holds only affect automatic and bulk updates; a row can still install by hand. */
fun eligibleForUpdate(packageName: String, updateAvailable: Boolean, heldPackages: Set<String>): Boolean =
    updateAvailable && packageName !in heldPackages
