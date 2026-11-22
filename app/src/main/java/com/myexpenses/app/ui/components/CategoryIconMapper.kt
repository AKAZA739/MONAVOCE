package com.myexpenses.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "restaurant" to Icons.Rounded.Restaurant,
        "shopping_basket" to Icons.Rounded.ShoppingBasket,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "receipt_long" to Icons.AutoMirrored.Rounded.ReceiptLong,
        "shopping_bag" to Icons.Rounded.ShoppingBag,
        "medical_services" to Icons.Rounded.MedicalServices,
        "sports_esports" to Icons.Rounded.SportsEsports,
        "category" to Icons.Rounded.Category,
        "local_cafe" to Icons.Rounded.LocalCafe,
        "home" to Icons.Rounded.Home,
        "school" to Icons.Rounded.School,
        "work" to Icons.Rounded.Work,
        "volunteer" to Icons.Rounded.VolunteerActivism,
        "pets" to Icons.Rounded.Pets
    )

    val availableColors: List<Long> = listOf(
        0xFFE67E22,
        0xFF16A085,
        0xFF2980B9,
        0xFF8E44AD,
        0xFFD81B60,
        0xFFE74C3C,
        0xFF00ACC1,
        0xFF607D8B,
        0xFF27AE60,
        0xFFF39C12,
        0xFF5C6BC0,
        0xFF795548
    )

    fun iconFor(name: String): ImageVector =
        availableIcons.firstOrNull { it.first == name }?.second ?: Icons.Rounded.Category
}
