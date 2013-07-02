package org.dbpedia.spotlight.entityTopic;

import info.bliki.htmlcleaner.ContentToken;
import info.bliki.htmlcleaner.TagNode;
import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.WPList;
import info.bliki.wiki.filter.WPTable;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.model.ImageFormat;
import info.bliki.wiki.model.WikiModel;
import info.bliki.wiki.tags.WPATag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse mediawiki markup to strip the formatting info and extract a simple text
 * version suitable for NLP along with header, paragraph and link position
 * annotations.
 *
 * Use the {@code #convert(String)} and {@code #getWikiLinks()} methods.
 *
 * Due to the constraints imposed by the {@code ITextConverter} /
 * {@code WikiModel} API, this class is not thread safe: only one instance
 * should be run by thread.
 */
public class AnnotatingMarkupParser implements ITextConverter {

    public static final String HREF_ATTR_KEY = "href";

    public static final String WIKILINK_TITLE_ATTR_KEY = "title";

    public static final String WIKILINK_TARGET_ATTR_KEY = "href";

    public static final String WIKIOBJECT_ATTR_KEY = "wikiobject";

    public static final Set<String> PARAGRAPH_TAGS = new HashSet<String>(
            Arrays.asList("p"));

    public static final Set<String> HEADING_TAGS = new HashSet<String>(
            Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6"));

    public static final Pattern INTERWIKI_PATTERN = Pattern.compile("http://[\\w-]+\\.wikipedia\\.org/wiki/.*");

    protected final List<Annotation> wikilinks = new ArrayList<Annotation>();

    protected final List<Annotation> headers = new ArrayList<Annotation>();

    protected final List<Annotation> paragraphs = new ArrayList<Annotation>();

    protected String languageCode;

    protected final WikiModel model;

    protected String redirect;

    protected String text;

    protected static final Map<String, Pattern> REDIRECT_PATTERNS = getRedirectPatterns();
    protected Pattern redirectPattern;


    private static Map<String, Pattern> getRedirectPatterns() {
        Map<String, Pattern> m = new HashMap<String, Pattern>();
        m.put("af", Pattern.compile("^(?:#AANSTUUR|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ar", Pattern.compile("^(?:#تحويل|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("arz", Pattern.compile("^(?:#تحويل|#تحويل|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("be-tarask", Pattern.compile("^(?:#перанакіраваньне|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("bg", Pattern.compile("^(?:#пренасочване|#виж|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("br", Pattern.compile("^(?:#ADKAS|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("bs", Pattern.compile("^(?:#PREUSMJERI|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("cs", Pattern.compile("^(?:#REDIRECT|#PŘESMĚRUJ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("cu", Pattern.compile("^(?:#ПРѢНАПРАВЛЄНИѤ|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("cy", Pattern.compile("^(?:#ail-cyfeirio|#ailgyfeirio|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("de", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("el", Pattern.compile("^(?:#ΑΝΑΚΑΤΕΥΘΥΝΣΗ|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("en", Pattern.compile("^(?:#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("eo", Pattern.compile("^(?:#ALIDIREKTU|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("es", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("et", Pattern.compile("^(?:#suuna|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("eu", Pattern.compile("^(?:#BIRZUZENDU|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("fa", Pattern.compile("^(?:#تغییرمسیر|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("fi", Pattern.compile("^(?:#OHJAUS|#UUDELLEENOHJAUS|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("fr", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ga", Pattern.compile("^(?:#redirect|#athsheoladh) \\[\\[([^\\]]*)\\]\\]"));
        m.put("gl", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("he", Pattern.compile("^(?:#הפניה|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("hr", Pattern.compile("^(?:#PREUSMJERI|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("hu", Pattern.compile("^(?:#ÁTIRÁNYÍTÁS|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("hy", Pattern.compile("^(?:#REDIRECT|#ՎԵՐԱՀՂՈՒՄ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("id", Pattern.compile("^(?:#ALIH|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("is", Pattern.compile("^(?:#tilvísun|#TILVÍSUN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ja", Pattern.compile("^(?:#転送|#リダイレクト|＃転送|＃リダイレクト|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ka", Pattern.compile("^(?:#REDIRECT|#გადამისამართება) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kk-arab", Pattern.compile("^(?:#REDIRECT|#ايداۋ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kk-cyrl", Pattern.compile("^(?:#REDIRECT|#АЙДАУ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kk-latn", Pattern.compile("^(?:#REDIRECT|#AÝDAW) \\[\\[([^\\]]*)\\]\\]"));
        m.put("km", Pattern.compile("^(?:#បញ្ជូនបន្ត|#ប្ដូរទីតាំងទៅ #ប្តូរទីតាំងទៅ|#ប្ដូរទីតាំង|#ប្តូរទីតាំង|#ប្ដូរចំណងជើង|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ko", Pattern.compile("^(?:#넘겨주기|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ksh", Pattern.compile("^(?:#ÖMLEIDUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("lt", Pattern.compile("^(?:#PERADRESAVIMAS|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("mk", Pattern.compile("^(?:#пренасочување|#види|#Пренасочување|#ПРЕНАСОЧУВАЊЕ|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ml", Pattern.compile("^(?:#REDIRECT|#തിരിച്ചുവിടുക|തിരിച്ചുവിടല്‍) \\[\\[([^\\]]*)\\]\\]"));
        m.put("mr", Pattern.compile("^(?:#पुनर्निर्देशन|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("mt", Pattern.compile("^(?:#RINDIRIZZA|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("mwl", Pattern.compile("^(?:#ANCAMINAR|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("nds", Pattern.compile("^(?:#redirect|#wiederleiden) \\[\\[([^\\]]*)\\]\\]"));
        m.put("nds-nl", Pattern.compile("^(?:#DEURVERWIEZING|#DOORVERWIJZING|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("nl", Pattern.compile("^(?:#DOORVERWIJZING|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("nn", Pattern.compile("^(?:#omdiriger|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("oc", Pattern.compile("^(?:#REDIRECCION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("pl", Pattern.compile("^(?:#PATRZ|#PRZEKIERUJ|#TAM|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("pt", Pattern.compile("^(?:#REDIRECIONAMENTO|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ro", Pattern.compile("^(?:#REDIRECTEAZA|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ru", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sa", Pattern.compile("^(?:#पुनर्निदेशन|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sd", Pattern.compile("^(?:#چوريو|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("si", Pattern.compile("^(?:#යළියොමුව|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sk", Pattern.compile("^(?:#presmeruj|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sq", Pattern.compile("^(?:#RIDREJTO|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("srn", Pattern.compile("^(?:#STIR|#DOORVERWIJZING|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sr-ec", Pattern.compile("^(?:#Преусмери|#redirect|#преусмери|#ПРЕУСМЕРИ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sr-el", Pattern.compile("^(?:#Preusmeri|#redirect|#preusmeri|#PREUSMERI) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sv", Pattern.compile("^(?:#OMDIRIGERING|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ta", Pattern.compile("^(?:#வழிமாற்று|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("te", Pattern.compile("^(?:#దారిమార్పు|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("th", Pattern.compile("^(?:#เปลี่ยนทาง|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("tr", Pattern.compile("^(?:#YÖNLENDİRME|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("tt-latn", Pattern.compile("^(?:#yünältü|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("uk", Pattern.compile("^(?:#ПЕРЕНАПРАВЛЕННЯ|#ПЕРЕНАПР|#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("vi", Pattern.compile("^(?:#đổi|#đổi|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("vro", Pattern.compile("^(?:#saadaq|#suuna|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("yi", Pattern.compile("^(?:#ווייטערפירן|#הפניה|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ab", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ace", Pattern.compile("^(?:#ALIH|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("aln", Pattern.compile("^(?:#RIDREJTO|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("als", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("an", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("arn", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("av", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ay", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ba", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("bar", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("bat-smg", Pattern.compile("^(?:#PERADRESAVIMAS|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("bcc", Pattern.compile("^(?:#تغییرمسیر|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("be-x-old", Pattern.compile("^(?:#перанакіраваньне|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("bm", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("bqi", Pattern.compile("^(?:#تغییرمسیر|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("bug", Pattern.compile("^(?:#ALIH|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("cbk-zam", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ce", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("crh-cyrl", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("cv", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("de-at", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("de-ch", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("de-formal", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("dsb", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ff", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("fiu-vro", Pattern.compile("^(?:#saadaq|#suuna|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("frp", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("gag", Pattern.compile("^(?:#YÖNLENDİRME|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("glk", Pattern.compile("^(?:#تغییرمسیر|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("gn", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("gsw", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("hsb", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ht", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("inh", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("jv", Pattern.compile("^(?:#ALIH|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kaa", Pattern.compile("^(?:#REDIRECT|#AÝDAW) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kk", Pattern.compile("^(?:#REDIRECT|#АЙДАУ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kk-cn", Pattern.compile("^(?:#REDIRECT|#ايداۋ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kk-kz", Pattern.compile("^(?:#REDIRECT|#АЙДАУ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kk-tr", Pattern.compile("^(?:#REDIRECT|#AÝDAW) \\[\\[([^\\]]*)\\]\\]"));
        m.put("kv", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("lad", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("lb", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("lbe", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("li", Pattern.compile("^(?:#DOORVERWIJZING|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ln", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("map-bms", Pattern.compile("^(?:#ALIH|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("mg", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("mhr", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("mo", Pattern.compile("^(?:#REDIRECTEAZA|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("myv", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("mzn", Pattern.compile("^(?:#تغییرمسیر|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("nah", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("os", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("pdc", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("qu", Pattern.compile("^(?:#REDIRECCIÓN|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("rmy", Pattern.compile("^(?:#REDIRECTEAZA|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ruq", Pattern.compile("^(?:#REDIRECTEAZA|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ruq-cyrl", Pattern.compile("^(?:#пренасочување|#види|#Пренасочување|#ПРЕНАСОЧУВАЊЕ|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ruq-grek", Pattern.compile("^(?:#ΑΝΑΚΑΤΕΥΘΥΝΣΗ|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ruq-latn", Pattern.compile("^(?:#REDIRECTEAZA|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sah", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("shi", Pattern.compile("^(?:#تحويل|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("simple", Pattern.compile("^(?:#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("sr", Pattern.compile("^(?:#Преусмери|#redirect|#преусмери|#ПРЕУСМЕРИ) \\[\\[([^\\]]*)\\]\\]"));
        m.put("stq", Pattern.compile("^(?:#WEITERLEITUNG|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("su", Pattern.compile("^(?:#ALIH|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("szl", Pattern.compile("^(?:#PATRZ|#PRZEKIERUJ|#TAM|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("tt", Pattern.compile("^(?:#yünältü|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("tt-cyrl", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ty", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("udm", Pattern.compile("^(?:#перенаправление|#перенапр|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("vep", Pattern.compile("^(?:#suuna|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("vls", Pattern.compile("^(?:#DOORVERWIJZING|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("wa", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("wo", Pattern.compile("^(?:#REDIRECTION|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("xmf", Pattern.compile("^(?:#REDIRECT|#გადამისამართება) \\[\\[([^\\]]*)\\]\\]"));
        m.put("ydd", Pattern.compile("^(?:#ווייטערפירן|#הפניה|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        m.put("zea", Pattern.compile("^(?:#DOORVERWIJZING|#REDIRECT) \\[\\[([^\\]]*)\\]\\]"));
        return m;
    }

    //TODO: fix for compatibility with other languages (assume English)
    public AnnotatingMarkupParser() {
        this("en");
    }

    public AnnotatingMarkupParser(String languageCode) {
        this.languageCode = languageCode;
        redirectPattern = REDIRECT_PATTERNS.get(languageCode);
        if(redirectPattern==null)
            redirectPattern = REDIRECT_PATTERNS.get("en");
        model = makeWikiModel(languageCode);
    }

    public WikiModel makeWikiModel(String languageCode) {
        return new WikiModel(String.format(
                "http:/%s.wikipedia.org/wiki/${image}", languageCode),
                String.format("http://%s.wikipedia.org/wiki/${title}",
                        languageCode)) {
            @Override
            public String getRawWikiContent(String namespace,
                                            String articleName, Map<String, String> templateParameters) {
                // disable template support
                // TODO: we need to read template support at least for dates
                return "";
            }
        };
    }

    /**
     * Convert WikiMarkup to a simple text representation suitable for NLP
     * analysis. The links encountered during the extraction are then available
     * by calling {@code #getWikiLinks()}.
     *
     * @param rawWikiMarkup
     * @return the simple text without the markup
     */
    public String parse(String rawWikiMarkup) {
        Matcher matcher = redirectPattern.matcher(rawWikiMarkup);
        if (matcher.find()) {
            redirect = titleToUri(matcher.group(1), languageCode);
        } else {
            redirect = null;
        }
        wikilinks.clear();
        headers.clear();
        paragraphs.clear();
        text = model.render(this, rawWikiMarkup);
        return text;
    }

    public static String titleToUri(String title, String languageCode) {
        try {
            return String.format("http://%s.wikipedia.org/wiki/%s",
                    languageCode,
                    URLEncoder.encode(title.replaceAll(" ", "_"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void nodesToText(List<? extends Object> nodes, Appendable buffer,
                            IWikiModel model) throws IOException {
        CountingAppendable countingBuffer;
        if (buffer instanceof CountingAppendable) {
            countingBuffer = (CountingAppendable) buffer;
        } else {
            // wrap
            countingBuffer = new CountingAppendable(buffer);
        }

        if (nodes != null && !nodes.isEmpty()) {
            try {
                int level = model.incrementRecursionLevel();
                if (level > Configuration.RENDERER_RECURSION_LIMIT) {
                    countingBuffer.append("Error - recursion limit exceeded"
                            + " rendering tags in PlainTextConverter#nodesToText().");
                    return;
                }
                for (Object node : nodes) {
                    if (node instanceof WPATag) {
                        // extract wikilink annotations
                        WPATag tag = (WPATag) node;
                        String wikilinkLabel = (String) tag.getAttributes().get(
                                WIKILINK_TITLE_ATTR_KEY);
                        String wikilinkTarget = (String) tag.getAttributes().get(
                                WIKILINK_TARGET_ATTR_KEY);
                        if (wikilinkLabel != null) {
                            int colonIdx = wikilinkLabel.indexOf(':');
                            if (colonIdx == -1) {
                                // do not serialize non-topic wiki-links such as
                                // translation links missing from the
                                // INTERWIKI_LINK map
                                int start = countingBuffer.currentPosition;
                                tag.getBodyString(countingBuffer);
                                int end = countingBuffer.currentPosition;
                                if (!wikilinkTarget.startsWith("#")) {
                                    wikilinks.add(new Annotation(start, end, wikilinkLabel, wikilinkTarget));
                                }
                            }
                        } else {
                            tag.getBodyString(countingBuffer);
                        }

                    } else if (node instanceof ContentToken) {
                        ContentToken contentToken = (ContentToken) node;
                        countingBuffer.append(contentToken.getContent());
                    } else if (node instanceof List) {
                    } else if (node instanceof WPList) {
                    } else if (node instanceof WPTable) {
                        // ignore lists and tables since they most of the time
                        // do not hold grammatically correct
                        // interesting sentences that are representative of the
                        // language.
                    } else if (node instanceof TagNode) {
                        TagNode tagNode = (TagNode) node;
                        Map<String, String> attributes = tagNode.getAttributes();
                        Map<String, Object> oAttributes = tagNode.getObjectAttributes();
                        boolean hasSpecialHandling = false;
                        String tagName = tagNode.getName();
                        int tagBegin = countingBuffer.currentPosition;
                        if ("a".equals(tagName)) {
                            String href = attributes.get(HREF_ATTR_KEY);
                            if (href != null
                                    && INTERWIKI_PATTERN.matcher(href).matches()) {
                                // ignore the interwiki links since they are
                                // mostly used for translation purpose.
                                hasSpecialHandling = true;
                            }
                        } else if ("ref".equals(tagName)) {
                            // ignore the references since they do not hold
                            // interesting text content
                            hasSpecialHandling = true;
                        } else if (oAttributes != null
                                && oAttributes.get(WIKIOBJECT_ATTR_KEY) instanceof ImageFormat) {
                            // the caption of images often holds well formed
                            // sentences with links to entities
                            hasSpecialHandling = true;
                            ImageFormat iformat = (ImageFormat) oAttributes.get(WIKIOBJECT_ATTR_KEY);
                            imageNodeToText(tagNode, iformat, countingBuffer,
                                    model);
                        }
                        if (!hasSpecialHandling) {
                            nodesToText(tagNode.getChildren(), countingBuffer,
                                    model);
                        }
                        if (PARAGRAPH_TAGS.contains(tagName)) {
                            paragraphs.add(new Annotation(tagBegin,
                                    countingBuffer.currentPosition,
                                    "paragraph", tagName));
                            countingBuffer.append("\n");
                        } else if (HEADING_TAGS.contains(tagName)) {
                            headers.add(new Annotation(tagBegin,
                                    countingBuffer.currentPosition, "heading",
                                    tagName));
                            countingBuffer.append("\n\n");
                        }
                    }
                }
            } finally {
                model.decrementRecursionLevel();
            }
        }
    }

    public void imageNodeToText(TagNode tagNode, ImageFormat imageFormat,
                                Appendable buffer, IWikiModel model) throws IOException {
        nodesToText(tagNode.getChildren(), buffer, model);
    }

    public boolean noLinks() {
        return true;
    }

    public List<Annotation> getWikiLinkAnnotations() {
        return wikilinks;
    }

    public List<Annotation> getHeaderAnnotations() {
        return headers;
    }

    public List<Annotation> getParagraphAnnotations() {
        return paragraphs;
    }

    public List<String> getParagraphs() {
        List<String> texts = new ArrayList<String>();
        for (Annotation p : paragraphs) {
            texts.add(text.substring(p.begin, p.end));
        }
        return texts;
    }

    public List<String> getHeaders() {
        List<String> texts = new ArrayList<String>();
        for (Annotation h : headers) {
            texts.add(text.substring(h.begin, h.end));
        }
        return texts;
    }

    public String getRedirect() {
        return redirect;
    }

    public class CountingAppendable implements Appendable {

        public int currentPosition = 0;

        final protected Appendable wrappedBuffer;

        public CountingAppendable(Appendable wrappedBuffer) {
            this.wrappedBuffer = wrappedBuffer;
        }

        public Appendable append(CharSequence charSeq) throws IOException {
            currentPosition += charSeq.length();
            return wrappedBuffer.append(charSeq);
        }

        public Appendable append(char aChar) throws IOException {
            currentPosition += 1;
            return wrappedBuffer.append(aChar);
        }

        public Appendable append(CharSequence charSeq, int start, int end)
                throws IOException {
            currentPosition += end - start;
            return wrappedBuffer.append(charSeq, start, end);
        }

    }

}
