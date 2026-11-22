package com.myexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val iconName: String,
    val colorHex: Long,
    /** Comma-separated Arabic & English keywords for local offline categorization */
    val keywords: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
) {
    fun displayName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn

    fun keywordList(): List<String> =
        keywords.split(",", "،", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}

object DefaultCategories {
    const val FOOD = "food"
    const val GROCERIES = "groceries"
    const val TRANSPORT = "transport"
    const val BILLS = "bills"
    const val SHOPPING = "shopping"
    const val HEALTH = "health"
    const val ENTERTAINMENT = "entertainment"
    const val OTHER = "other"

    val list: List<CategoryEntity> = listOf(
        CategoryEntity(
            id = FOOD,
            nameAr = "الطعام والمطاعم",
            nameEn = "Food & Dining",
            iconName = "restaurant",
            colorHex = 0xFFE67E22,
            keywords = "طعام,أكل,اكل,الأكل,مطعم,مطاعم,فطور,غداء,غدا,عشاء,عشا,وجبة,قهوة,قهوه,شاي,شاهي,عصير,كافيه,مقهى,بيتزا,شاورما,برجر,فول,فلافل,كبسة,مندي,مظبي,سلتة,فحسة,عصيدة,بروست,ساندويتش,سندوتش,حلويات,حلا,كيك,آيسكريم,بوظة,دونات,معجنات,مخبز,مشويات,سمك مشوي,ديليفري,توصيل اكل,food,eat,eating,dining,restaurant,breakfast,lunch,dinner,meal,coffee,tea,cafe,juice,pizza,burger,shawarma,sandwich,snack,dessert,sweet,bakery,delivery",
            isDefault = true,
            sortOrder = 1
        ),
        CategoryEntity(
            id = GROCERIES,
            nameAr = "البقالة والمشتريات",
            nameEn = "Groceries",
            iconName = "shopping_basket",
            colorHex = 0xFF16A085,
            keywords = "بقالة,بقاله,سوبرماركت,سوبر ماركت,هايبر,تموينات,مقاضي,مشتريات,مشتروات,خضار,خضروات,فواكه,فاكهة,خبز,روتي,عيش,حليب,لبن,جبن,جبنة,بيض,سكر,أرز,رز,زيت,دقيق,طحين,لحم,دجاج,فراخ,تونة,معلبات,ماء,موية,مياه,قارورة,منظفات,صابون,شامبو,مناديل,سوق,خضرة,بطاط,طماطم,بصل,ثوم,grocery,groceries,supermarket,market,vegetables,veggies,fruit,fruits,bread,milk,cheese,eggs,sugar,rice,oil,meat,chicken,water,detergent,soap,tissue,supplies",
            isDefault = true,
            sortOrder = 2
        ),
        CategoryEntity(
            id = TRANSPORT,
            nameAr = "المواصلات والبنزين",
            nameEn = "Transport & Fuel",
            iconName = "directions_car",
            colorHex = 0xFF2980B9,
            keywords = "مواصلات,بنزين,بترول,ديزل,وقود,محطة,تاكسي,تكسي,أجرة,اجرة,باص,دباب,متر,أوبر,اوبر,كريم,مشوار,توصيل,سيارة,سياره,صيانة,ورشة,ميكانيكي,زيت سيارة,غيار زيت,بنشر,كفر,إطار,غسيل سيارة,موقف,باركنج,تذكرة,سفر,طيران,transport,transportation,fuel,gas,petrol,diesel,taxi,cab,bus,uber,careem,ride,car,maintenance,repair,parking,ticket,travel,flight,commute",
            isDefault = true,
            sortOrder = 3
        ),
        CategoryEntity(
            id = BILLS,
            nameAr = "الفواتير والإنترنت",
            nameEn = "Bills & Internet",
            iconName = "receipt_long",
            colorHex = 0xFF8E44AD,
            keywords = "فاتورة,فواتير,إنترنت,انترنت,نت,النت,واي فاي,وايفاي,باقة,رصيد,شحن,كروت,كرت,بطاقة شحن,كهرباء,كهربا,ماء,مياه,إيجار,ايجار,غاز,اسطوانة غاز,اتصالات,جوال,تلفون,هاتف,يمن موبايل,سبأفون,يو,واي,عدن نت,يمن نت,ستارلينك,اشتراك نت,bill,bills,internet,wifi,data,recharge,credit,topup,electricity,power,water bill,rent,gas cylinder,phone,mobile,utility,utilities,telecom",
            isDefault = true,
            sortOrder = 4
        ),
        CategoryEntity(
            id = SHOPPING,
            nameAr = "التسوق",
            nameEn = "Shopping",
            iconName = "shopping_bag",
            colorHex = 0xFFD81B60,
            keywords = "تسوق,ملابس,ثوب,قميص,بنطلون,تيشيرت,جاكيت,عباية,فستان,حذاء,جزمة,شوز,صندل,عطر,بخور,ساعة,شنطة,حقيبة,محفظة,نظارة شمسية,إلكترونيات,الكترونيات,شاحن,كيبل,سلك,سماعة,سماعات,كفر جوال,لابتوب,جهاز,أثاث,اثاث,أدوات,مكتبة,قرطاسية,كتاب,دفتر,قلم,هدية,هدايا,حلاقة,حلاق,صالون,shopping,clothes,clothing,shirt,pants,shoes,perfume,watch,bag,electronics,charger,headphones,cable,book,stationery,gift,furniture,haircut,barber,store,amazon",
            isDefault = true,
            sortOrder = 5
        ),
        CategoryEntity(
            id = HEALTH,
            nameAr = "الصحة",
            nameEn = "Health",
            iconName = "medical_services",
            colorHex = 0xFFE74C3C,
            keywords = "صحة,دواء,دوا,أدوية,علاج,صيدلية,صيدليه,دكتور,طبيب,مستشفى,مستوصف,عيادة,كشف,كشفية,فحص,تحليل,تحاليل,مختبر,أشعة,اشعة,أسنان,اسنان,نظارة طبية,فيتامين,فيتامينات,مسكن,مضاد,عملية,health,medicine,medication,pharmacy,doctor,hospital,clinic,checkup,lab,test,xray,dental,dentist,vitamin,medical,treatment",
            isDefault = true,
            sortOrder = 6
        ),
        CategoryEntity(
            id = ENTERTAINMENT,
            nameAr = "الترفيه",
            nameEn = "Entertainment",
            iconName = "sports_esports",
            colorHex = 0xFF00ACC1,
            keywords = "ترفيه,سينما,فيلم,ألعاب,العاب,لعبة,بلايستيشن,ببجي,شدات,شحن لعبة,اشتراك,نتفلكس,يوتيوب,سبوتيفاي,نزهة,رحلة,طلعة,تمشية,ملعب,كرة,كورة,مباراة,جيم,نادي,رياضة,سباحة,ملاهي,حديقة,entertainment,cinema,movie,film,game,games,gaming,playstation,netflix,youtube,spotify,subscription,outing,trip,picnic,football,soccer,gym,club,sport,park,fun",
            isDefault = true,
            sortOrder = 7
        ),
        CategoryEntity(
            id = OTHER,
            nameAr = "مصروفات أخرى",
            nameEn = "Other",
            iconName = "category",
            colorHex = 0xFF607D8B,
            keywords = "أخرى,اخرى,متنوع,متفرقات,صدقة,تبرع,سلف,دين,مصروف,نثريات,other,misc,miscellaneous,general,charity,donation",
            isDefault = true,
            sortOrder = 8
        )
    )
}
