package util;

import android.content.Context;

import com.dimxlp.managerdb.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NationalityFlagUtil {

    public static Map<String, String> getNationalityToIsoMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Argentina", "AR");
        map.put("Brazil", "BR");
        map.put("Germany", "DE");
        map.put("France", "FR");
        map.put("Greece", "GR");
        map.put("Italy", "IT");
        map.put("Spain", "ES");
        map.put("Portugal", "PT");
        map.put("England", "ENG");
        map.put("United States", "US");
        map.put("Canada", "CA");
        map.put("Mexico", "MX");
        map.put("Japan", "JP");
        map.put("South Korea", "KR");
        map.put("Australia", "AU");
        map.put("Nigeria", "NG");
        map.put("Ghana", "GH");
        map.put("Morocco", "MA");
        map.put("Netherlands", "NL");
        map.put("Belgium", "BE");
        map.put("Denmark", "DK");
        map.put("Sweden", "SE");
        map.put("Norway", "NO");
        map.put("Finland", "FI");
        map.put("Switzerland", "CH");
        map.put("Austria", "AT");
        map.put("Poland", "PL");
        map.put("Czechia", "CZ");
        map.put("Russia", "RU");
        map.put("Turkey", "TR");
        map.put("Croatia", "HR");
        map.put("Serbia", "RS");
        map.put("Romania", "RO");
        map.put("Ukraine", "UA");
        map.put("South Africa", "ZA");
        map.put("Egypt", "EG");
        map.put("Cameroon", "CM");
        map.put("Senegal", "SN");
        map.put("Chile", "CL");
        map.put("Colombia", "CO");
        map.put("Uruguay", "UY");
        map.put("Peru", "PE");
        map.put("Paraguay", "PY");
        map.put("Ecuador", "EC");
        map.put("Venezuela", "VE");
        map.put("China", "CN");
        map.put("India", "IN");
        map.put("New Zealand", "NZ");
        map.put("Saudi Arabia", "SA");
        map.put("Guatemala", "GT");
        map.put("Haiti", "HT");
        map.put("Dominican Republic", "DO");
        map.put("Cuba", "CU");
        map.put("Honduras", "HN");
        map.put("Nicaragua", "NI");
        map.put("El Salvador", "SV");
        map.put("Costa Rica", "CR");
        map.put("Panama", "PA");
        map.put("Jamaica", "JM");
        map.put("Trinidad and Tobago", "TT");
        map.put("Guyana", "GY");
        map.put("Suriname", "SR");
        map.put("Malta", "MT");
        map.put("San Marino", "SM");
        map.put("Andorra", "AD");
        map.put("Gibraltar", "GI");
        map.put("Cyprus", "CY");
        map.put("North Macedonia", "MK");
        map.put("Bosnia and Herzegovina", "BA");
        map.put("Montenegro", "ME");
        map.put("Kosovo", "XK");
        map.put("Uzbekistan", "UZ");
        map.put("Kazakhstan", "KZ");
        map.put("Pakistan", "PK");
        map.put("Afghanistan", "AF");
        map.put("Bangladesh", "BD");
        map.put("Sri Lanka", "LK");
        map.put("Nepal", "NP");
        map.put("Bhutan", "BT");
        map.put("Maldives", "MV");
        map.put("Indonesia", "ID");
        map.put("Vietnam", "VN");
        map.put("Philippines", "PH");
        map.put("Thailand", "TH");
        map.put("Malaysia", "MY");
        map.put("Singapore", "SG");
        map.put("Iraq", "IQ");
        map.put("Yemen", "YE");
        map.put("Syria", "SY");
        map.put("Jordan", "JO");
        map.put("UAE", "AE");
        map.put("Bahrain", "BH");
        map.put("Lebanon", "LB");
        map.put("Oman", "OM");
        map.put("Kuwait", "KW");
        map.put("Qatar", "QA");
        map.put("Iran", "IR");
        map.put("Sudan", "SD");
        map.put("Tunisia", "TN");
        map.put("Libya", "LY");
        map.put("Namibia", "NA");
        map.put("Lesotho", "LS");
        map.put("Eswatini", "SZ");
        map.put("DR Congo", "CD");
        map.put("Angola", "AO");
        map.put("Chad", "TD");
        map.put("Gabon", "GA");
        map.put("Rwanda", "RW");
        map.put("Niger", "NE");
        map.put("Mali", "ML");
        map.put("Burkina Faso", "BF");
        map.put("Guinea", "GN");
        map.put("Benin", "BJ");
        map.put("Togo", "TG");
        map.put("Sierra Leone", "SL");
        map.put("Liberia", "LR");
        map.put("Ethiopia", "ET");
        map.put("Kenya", "KE");
        map.put("Uganda", "UG");
        map.put("Tanzania", "TZ");
        map.put("Zambia", "ZM");
        map.put("Mozambique", "MZ");
        map.put("Malawi", "MW");
        map.put("Madagascar", "MG");
        map.put("Zimbabwe", "ZW");
        map.put("Papua New Guinea", "PG");
        map.put("Fiji", "FJ");
        map.put("Tonga", "TO");
        map.put("Vanuatu", "VU");
        map.put("Solomon Islands", "SB");
        map.put("Albania", "AL");
        map.put("Algeria", "DZ");
        map.put("Armenia", "AM");
        map.put("Azerbaijan", "AZ");
        map.put("Bahamas", "BS");
        map.put("Barbados", "BB");
        map.put("Belarus", "BY");
        map.put("Bolivia", "BO");
        map.put("Bulgaria", "BG");
        map.put("Ivory Coast", "CI");
        map.put("Estonia", "EE");
        map.put("Georgia", "GE");
        map.put("Hungary", "HU");
        map.put("Iceland", "IS");
        map.put("Ireland", "IE");
        map.put("Israel", "IL");
        map.put("North Korea", "KP");
        map.put("Latvia", "LV");
        map.put("Liechtenstein", "LI");
        map.put("Lithuania", "LT");
        map.put("Luxembourg", "LU");
        map.put("Moldova", "MD");
        map.put("Northern Ireland", "NIR");
        map.put("Palestine", "PS");
        map.put("Puerto Rico", "PR");
        map.put("Scotland", "SCT");
        map.put("Seychelles", "SC");
        map.put("Slovakia", "SK");
        map.put("Slovenia", "SI");
        map.put("Wales", "WLS");

        return map;
    }

    public static int getFlagResId(Context context, String isoCode) {
        if (isoCode == null) return R.drawable.flag_unknown;
        String resName = "flag_" + isoCode.toLowerCase(Locale.ROOT);
        int resId = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
        return resId == 0 ? R.drawable.flag_unknown : resId;
    }

    public static Map<String, String> getVariantToStandardMap() {
        Map<String, String> map = new HashMap<>();
        map.put("USA", "United States");
        map.put("U.S.A.", "United States");
        map.put("United States of America", "United States");
        map.put("UAE", "United Arab Emirates");
        map.put("U.A.E.", "United Arab Emirates");
        map.put("UK", "England");
        map.put("U.K.", "England");
        map.put("Great Britain", "England");
        map.put("Turkiye", "Turkey");
        map.put("Türkiye", "Turkey");
        map.put("Republic of Korea", "South Korea");
        map.put("Korea Republic", "South Korea");
        map.put("DPRK", "North Korea");
        map.put("Democratic People's Republic of Korea", "North Korea");
        map.put("Côte d'Ivoire", "Ivory Coast");
        map.put("Cote d'Ivoire", "Ivory Coast");
        map.put("Russian Federation", "Russia");
        map.put("Islamic Republic of Iran", "Iran");
        map.put("Syrian Arab Republic", "Syria");
        map.put("State of Palestine", "Palestine");
        map.put("Republic of Moldova", "Moldova");
        map.put("Bolivarian Republic of Venezuela", "Venezuela");
        map.put("Lao People's Democratic Republic", "Laos");
        map.put("Czech Republic", "Czechia");
        return map;
    }

}