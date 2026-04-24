package com.hfstudio.guidenh.guide.internal.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fa.PersianAnalyzer;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.ga.IrishAnalyzer;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;

final class Analyzers {

    private Analyzers() {}

    public static final String LANG_ARABIC = "ar";
    public static final String LANG_BRAZILIAN = "br";
    public static final String LANG_BULGARIAN = "bg";
    public static final String LANG_CATALAN = "ca";
    public static final String LANG_CHINESE = "cn";
    public static final String LANG_CZECH = "cz";
    public static final String LANG_DANISH = "da";
    public static final String LANG_DUTCH = "nl";
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_FARSI = "fa";
    public static final String LANG_FINNISH = "fi";
    public static final String LANG_FRENCH = "fr";
    public static final String LANG_GERMAN = "de";
    public static final String LANG_GREEK = "gr";
    public static final String LANG_HINDI = "hi";
    public static final String LANG_HUNGARIAN = "hu";
    public static final String LANG_IRISH = "ir";
    public static final String LANG_ITALIAN = "it";
    public static final String LANG_JAPANESE = "jp";
    public static final String LANG_KOREAN = "ko";
    public static final String LANG_NORWEGIAN = "no";
    public static final String LANG_PORTUGUESE = "pt";
    public static final String LANG_ROMANIAN = "ro";
    public static final String LANG_RUSSIAN = "ru";
    public static final String LANG_SPANISH = "es";
    public static final String LANG_SWEDISH = "sw";
    public static final String LANG_TURKISH = "tr";

    public static final List<String> LANGUAGES = Arrays.asList(
        LANG_ARABIC,
        LANG_BRAZILIAN,
        LANG_BULGARIAN,
        LANG_CATALAN,
        LANG_CHINESE,
        LANG_CZECH,
        LANG_DANISH,
        LANG_DUTCH,
        LANG_ENGLISH,
        LANG_FARSI,
        LANG_FINNISH,
        LANG_FRENCH,
        LANG_GERMAN,
        LANG_GREEK,
        LANG_HINDI,
        LANG_HUNGARIAN,
        LANG_IRISH,
        LANG_ITALIAN,
        LANG_JAPANESE,
        LANG_KOREAN,
        LANG_NORWEGIAN,
        LANG_PORTUGUESE,
        LANG_ROMANIAN,
        LANG_RUSSIAN,
        LANG_SPANISH,
        LANG_SWEDISH,
        LANG_TURKISH);

    public static final Map<String, String> MINECRAFT_TO_LUCENE_LANG;

    public static final Map<String, Supplier<Analyzer>> ANALYZERS;

    static {
        var map = new HashMap<String, Supplier<Analyzer>>();
        map.put(LANG_ARABIC, ArabicAnalyzer::new);
        map.put(LANG_BRAZILIAN, BrazilianAnalyzer::new);
        map.put(LANG_BULGARIAN, BulgarianAnalyzer::new);
        map.put(LANG_CATALAN, CatalanAnalyzer::new);
        map.put(LANG_CHINESE, CJKAnalyzer::new);
        map.put(LANG_CZECH, CzechAnalyzer::new);
        map.put(LANG_DANISH, DanishAnalyzer::new);
        map.put(LANG_DUTCH, DutchAnalyzer::new);
        map.put(LANG_ENGLISH, EnglishAnalyzer::new);
        map.put(LANG_FARSI, PersianAnalyzer::new);
        map.put(LANG_FINNISH, FinnishAnalyzer::new);
        map.put(LANG_FRENCH, FrenchAnalyzer::new);
        map.put(LANG_GERMAN, GermanAnalyzer::new);
        map.put(LANG_GREEK, GreekAnalyzer::new);
        map.put(LANG_HINDI, HindiAnalyzer::new);
        map.put(LANG_HUNGARIAN, HungarianAnalyzer::new);
        map.put(LANG_IRISH, IrishAnalyzer::new);
        map.put(LANG_ITALIAN, ItalianAnalyzer::new);
        map.put(LANG_JAPANESE, CJKAnalyzer::new);
        map.put(LANG_KOREAN, CJKAnalyzer::new);
        map.put(LANG_NORWEGIAN, NorwegianAnalyzer::new);
        map.put(LANG_PORTUGUESE, PortugueseAnalyzer::new);
        map.put(LANG_ROMANIAN, RomanianAnalyzer::new);
        map.put(LANG_RUSSIAN, RussianAnalyzer::new);
        map.put(LANG_SPANISH, SpanishAnalyzer::new);
        map.put(LANG_SWEDISH, SwedishAnalyzer::new);
        map.put(LANG_TURKISH, TurkishAnalyzer::new);
        ANALYZERS = Collections.unmodifiableMap(new HashMap<>(map));

        var langMap = new HashMap<String, String>();
        langMap.put("ar_sa", LANG_ARABIC);
        langMap.put("bg_bg", LANG_BULGARIAN);
        langMap.put("pt_br", LANG_BRAZILIAN);
        langMap.put("en_us", LANG_ENGLISH);
        langMap.put("de_de", LANG_GERMAN);
        langMap.put("ca_es", LANG_CATALAN);
        langMap.put("zh_cn", LANG_CHINESE);
        langMap.put("zh_hk", LANG_CHINESE);
        langMap.put("zh_tw", LANG_CHINESE);
        langMap.put("ja_jp", LANG_JAPANESE);
        langMap.put("ko_kr", LANG_KOREAN);
        langMap.put("cs_cz", LANG_CZECH);
        langMap.put("da_dk", LANG_DANISH);
        langMap.put("el_gr", LANG_GREEK);
        langMap.put("es_es", LANG_SPANISH);
        langMap.put("fa_ir", LANG_FARSI);
        langMap.put("fi_fi", LANG_FINNISH);
        langMap.put("fr_fr", LANG_FRENCH);
        langMap.put("ga_ie", LANG_IRISH);
        langMap.put("hi_in", LANG_HINDI);
        langMap.put("hu_hu", LANG_HUNGARIAN);
        langMap.put("it_it", LANG_ITALIAN);
        langMap.put("nl_nl", LANG_DUTCH);
        langMap.put("no_no", LANG_NORWEGIAN);
        langMap.put("pt_pt", LANG_PORTUGUESE);
        langMap.put("ro_ro", LANG_ROMANIAN);
        langMap.put("ru_ru", LANG_RUSSIAN);
        langMap.put("sv_se", LANG_SWEDISH);
        langMap.put("tr_tr", LANG_TURKISH);
        MINECRAFT_TO_LUCENE_LANG = Collections.unmodifiableMap(new HashMap<>(langMap));
    }
}
