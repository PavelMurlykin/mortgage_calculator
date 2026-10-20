# User manual

## Installation and launch

The app supports Android 7.0 and newer (API 24+). Install an APK from
[Releases](https://github.com/PavelMurlykin/mortgage_calculator/releases) and open Mortgage Calculator.

To build it, open the project in Android Studio with AGP 9.4.1 support,
Android SDK 35 (Build Tools 36.0.0) and Eclipse Temurin JDK 21. The project uses AGP 9.4.1,
Kotlin 2.2.10, KSP 2.3.6 and Room 2.7.2 from `gradle/libs.versions.toml`,
plus Gradle 9.6.0 through the Gradle Wrapper;
no separate Gradle installation is needed. After changing versions, run
`Sync Project with Gradle Files` in Android Studio. The AGP version is defined by
the `agp` key in the version catalog, with plugin aliases used in the build files.
Select Eclipse Temurin 21 as the Gradle JDK; this JDK is also specified
in `gradle/gradle-daemon-jvm.properties`.
Create a local `gradle.properties` file in the project root (it is excluded from Git):

```properties
android.builtInKotlin=false
android.newDsl=false
android.useAndroidX=true
org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8
```

The first two settings keep the current Kotlin plugin and DSL compatible with AGP 9.
AGP warns that they are deprecated; they remain supported by the current version,
but moving to AGP 10 will require a separate migration. Room 2.7.2 supports KSP2;
reverting to Room 2.6.1 causes `unexpected jvm signature V` when processing DAOs.
Updating Room does not change the database schema or require deleting saved calculations.

On Windows run `./gradlew.bat assembleDebug`; on macOS/Linux run `sh ./gradlew assembleDebug`.
The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Install it on a device or launch the app from Android Studio.
To check calculation logic, run `./gradlew.bat testDebugUnitTest` (Windows) or
`sh ./gradlew testDebugUnitTest` (macOS/Linux).

## Calculation

1. Open Calculation («Расчет»). The default mode calculates a monthly payment.
2. Enter the property value, down payment, term and annual interest rate.
3. Choose annuity or differentiated payments in the result card.
4. Results update automatically. Schedule («График >») opens the monthly payments.

The down payment can be edited in rubles or percent. The lock icon identifies the
last selected representation, which is retained when the property value changes.
The term is entered in years or total months: these are two representations of one
term, rather than parts to add together. Editing years sets a whole number of years;
editing months allows partial years. The allowed term is 0–30 years
(360 months). With a zero term, the payment is zero and the schedule is empty.

Rates range from 0 to 100%, inclusive. Use the keyboard or the minus and plus buttons
to edit values. The term changes in steps of one year or one month.
Monetary results are displayed in whole rubles; calculations do not round to whole rubles.
For differentiated payments, the result shows the first payment of the relevant period.

In Settings, select Property value («Стоимость объекта») to enter a desired monthly
payment, down payment, term and rate and calculate the property value.
Saving and the schedule are available in Monthly payment («Ежемесячный платеж») mode.

## Additional parameters

Settings → Additional parameters («Настройки» → «Дополнительные параметры») has two switches:

- Discount/Markup («Скидка/Удорожание») adds a property price adjustment in rubles
  and percent to the calculation screen. It applies in Monthly payment mode.
- Grace period («Льготный период») adds Grace period term («Срок льготного периода»)
  and Grace interest rate («Льготная процентная ставка») at the bottom of Calculation.
  The switch is off by default, with a zero term and rate.

Enter the grace term in years or total months using the same rules as the main term.
It cannot exceed the mortgage term. Shortening the mortgage automatically reduces
the grace term to the new mortgage term when needed. The grace rate allows 0–100%
and uses the same increment as the main rate. The term and rate survive app restarts.

When grace is enabled and its term is positive, Monthly grace payment
(«Ежемесячный льготный платеж») appears above the main result. For annuity payments,
this payment uses the grace rate over the full mortgage term. After the grace period,
the outstanding debt is recalculated at the main rate over the remaining term.
For differentiated payments, principal is repaid evenly throughout the term and
the interest rate changes at the period boundary. Monthly payment («Ежемесячный платеж»)
shows the first payment after the grace period.

For example, a 1,200,000 ₽ loan over 24 months with a 0% grace rate for the first
12 months has a monthly annuity grace payment of 50,000 ₽. The remaining
600,000 ₽ is then repaid over 12 months at the main rate.

If the grace period covers the full mortgage term, all payments use the grace rate.
The regular payment is displayed as 0 ₽ with a note that no payments follow the grace period.
In Property value mode, the entered payment means the first payment after the grace
period, or the grace payment if the grace period covers the entire mortgage term.
With the switch off or a zero grace term, the ordinary calculation applies.
Turning the switch off retains the grace inputs for later use.

## Schedule, saved calculations and settings

Tap a year in the schedule to expand or collapse its months. Each month shows the
payment and outstanding debt. Grace months are marked «Льготный» (Grace).
The first payment is dated in the month following the current date.

Tap the save icon on Calculation. In List («Список»), each saved calculation shows
its main parameters and, when an enabled grace period has a positive term, its grace
payment, term and rate. Terms are saved to the nearest entered month. Existing saved
calculations continue to work without a grace period. Tap the title to rename it,
the pencil to load its parameters into Calculation, or the trash icon to delete it.
Loading also restores the additional parameter switches.

Settings → Modifiers («Настройки» → «Модификаторы») changes the amount increment
(default 100,000 ₽), payment increment (10,000 ₽), and down payment/rate increment
(0.1%; choices are 1%, 0.1%, 0.01%). Settings and current inputs are stored on the device.
