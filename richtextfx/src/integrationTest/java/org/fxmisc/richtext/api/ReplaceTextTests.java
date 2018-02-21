package org.fxmisc.richtext.api;

import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

public class ReplaceTextTests extends InlineCssTextAreaAppTest {

    @Test
    public void deselect_before_replaceText_does_not_cause_index_out_of_bounds_exception()
    {
        interact( () ->
        {
            area.replaceText( "1234567890\n\nabcdefghij\n\n1234567890\n\nabcdefghij" );

            // Select last line of text.
            area.requestFollowCaret();
            area.selectLine();

            // Calling deselect, primes an IndexOutOfBoundsException to be thrown after replaceText
            area.deselect();

            // An internal IndexOutOfBoundsException may occur in ParagraphText.getRangeShapeSafely
            area.replaceText( "1234567890\n\nabcdefghijklmno\n\n1234567890\n\nabcde" );

            // This would fail if an exception occurred during ParagraphText.layoutChildren:updateSelectionShape().
            area.selectLine();
        });

    }

    @Test
    public void previous_selection_before_replaceText_does_not_cause_index_out_of_bounds_exception()
    {
        interact( () ->
        {
        	// For this test to work the area MUST be at the end of the document.
        	area.requestFollowCaret();

            // Text supplied by bug reporter contains a and o with diaereses/umlaut
            area.replaceText( getTextA() );

            // Any text can be selected anywhere in the document.
            area.selectWord();

            // Text supplied by bug reporter contains a and o with diaereses/umlaut
            area.replaceText( getTextB() );

            // An internal IndexOutOfBoundsException may occur in ParagraphText.getRangeShapeSafely
            area.replaceText( getTextA() );

            // This would fail if an exception occurred during ParagraphText.layoutChildren:updateSelectionShape().
            area.selectLine();
        });
    }

    private String getTextA()
    {
        return "<!DOCTYPE HTML>\n" +
                "<html xmlns:og=\"http://ogp.me/ns#\" xmlns:fb=\"http://www.facebook.com/2008/fbml\" lang=\"fi\">\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<meta name=\"theme-color\" content=\"#000\">\n" +
                "<meta name=\"google-site-verification\" content=\"-l8cEpWwFrNL61eaBmGNVLxBytB_LBWT6cESeZc4s-E\" />\n" +
                "<meta name=\"google-signin-client_id\" content=\"776208230589-2etb5qlg2ifkfrrqj9nrhje6dckbu4qo.apps.googleusercontent.com\">\n" +
                "\n" +
                "<title>\n" +
                "    Parempaa kuvaa ja &auml;&auml;nentoistoa jo vuodesta 1981 - HifiStudio</title>\n" +
                "\n" +
                "<base href=\"https://www.hifistudio.fi/fi/\">\n" +
                "<meta property=\"og:title\" content=\"HifiStudio - Parempaa kuvaa ja &auml;&auml;nentoistoa jo vuodesta 1981\" />\n" +
                "<meta property=\"og:type\" content=\"company\" />\n" +
                "\n" +
                "<meta property=\"page_type\" content=\"other\" />\n" +
                "<meta property=\"og:image\" content=\"https://www.hifistudio.fi/fi/gfx/common/hifi_fb_200x200.jpg\" />\n" +
                "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" />\n" +
                "<meta property=\"og:url\" content=\"https://www.hifistudio.fi/fi/\" />\n" +
                "<meta name=\"Description\" content=\"HifiStudio - tuotemerkkien ja tuotteiden esittely, verkkokauppa sek&auml; yritysinfo ja palvelut.\" />\n" +
                "<meta name=\"Keywords\" content=\"HifiStudio, Hifi, Hifi Studio, kaiuttimet, kaapelit, kuulokkeet, kotiteatteri, vahvistimet, viritinvahvistimet, levysoittimet, design, laitetelineet\" />\n" +
                "\n" +
                "<link rel=\"shortcut icon\" href=\"gfx/common/favicon.ico\" />\n" +
                "<link rel=\"stylesheet\" href=\"css/hifistudio_bootstrap.css\">\n" +
                "<link rel=\"stylesheet\" href=\"css/flexslider.css\">\n" +
                "<link rel=\"stylesheet\" href=\"css/easy-autocomplete.min.css\">\n" +
                "<link rel=\"stylesheet\" href=\"css/hifistudio17.css?version=7.7\">\n" +
                "<script src=\"https://code.jquery.com/jquery-1.12.4.min.js\"></script>\n" +
                "<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>\n" +
                "<script src=\"js/hifistudio17.js?version=7.7\"></script>\n" +
                "<script src=\"js/jquery.easy-autocomplete.min.js\"></script>\n" +
                "<script src=\"js/jquery.flexslider-min.js\"></script>\n" +
                "<script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':\n" +
                "new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],\n" +
                "j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=\n" +
                "'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);\n" +
                "})(window,document,'script','dataLayer','GTM-PXGN7ZT');</script>\n" +
                "\n" +
                "<script>\n" +
                "    var _gaq = _gaq || [];\n" +
                "    _gaq.push([ '_setAccount', 'UA-178451-1' ]);\n" +
                "    _gaq.push([ '_trackPageview' ]);\n" +
                "    (function() {\n" +
                "        var ga = document.createElement('script');\n" +
                "        ga.type = 'text/javascript';\n" +
                "        ga.async = true;\n" +
                "        ga.src = ('https:' == document.location.protocol ? 'https://ssl'\n" +
                "                : 'http://www')\n" +
                "                + '.google-analytics.com/ga.js';\n" +
                "        var s = document.getElementsByTagName('script')[0];\n" +
                "        s.parentNode.insertBefore(ga, s);\n" +
                "    })();\n" +
                "</script>\n" +
                "<script src=\"https://apis.google.com/js/platform.js\" async defer></script>\n" +
                "<script>\n" +
                "(function(w, t, f) {\n" +
                "  var s='script',o='_giosg',h='https://service.giosg.com',e,n;e=t.createElement(s);e.async=1;e.src=h+'/live/';\n" +
                "  w[o]=w[o]||function(){(w[o]._e=w[o]._e||[]).push(arguments)};w[o]._c=f;w[o]._h=h;n=t.getElementsByTagName(s)[0];n.parentNode.insertBefore(e,n);\n" +
                "})(window,document,3057);\n" +
                "</script>\n" +
                "<script>\n" +
                "    !function(f, b, e, v, n, t, s) {\n" +
                "        if (f.fbq)\n" +
                "            return;\n" +
                "        n = f.fbq = function() {\n" +
                "            n.callMethod ? n.callMethod.apply(n, arguments) : n.queue\n" +
                "                    .push(arguments)\n" +
                "        };\n" +
                "        if (!f._fbq)\n" +
                "            f._fbq = n;\n" +
                "        n.push = n;\n" +
                "        n.loaded = !0;\n" +
                "        n.version = '2.0';\n" +
                "        n.queue = [];\n" +
                "        t = b.createElement(e);\n" +
                "        t.async = !0;\n" +
                "        t.src = v;\n" +
                "        s = b.getElementsByTagName(e)[0];\n" +
                "        s.parentNode.insertBefore(t, s)\n" +
                "    }(window, document, 'script', '//connect.facebook.net/en_US/fbevents.js');\n" +
                "\n" +
                "    fbq('init', '871037816322217');\n" +
                "    fbq('track', \"PageView\");\n" +
                "</script>\n" +
                "<script>\n" +
                "!function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?\n" +
                "n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;\n" +
                "n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;\n" +
                "t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,\n" +
                "document,'script','https://connect.facebook.net/en_US/fbevents.js');\n" +
                "fbq('init', '115621915683882'); // Insert your pixel ID here.\n" +
                "fbq('track', 'PageView');\n" +
                "</script>\n" +
                "<noscript><img height=\"1\" width=\"1\" style=\"display:none\"\n" +
                "src=\"https://www.facebook.com/tr?id=115621915683882&ev=PageView&noscript=1\"\n" +
                "/></noscript>\n" +
                "<script>(function() {\n" +
                "        var _fbq = window._fbq || (window._fbq = []);\n" +
                "        if (!_fbq.loaded) {\n" +
                "            var fbds = document.createElement('script');\n" +
                "            fbds.async = true;\n" +
                "            fbds.src = '//connect.facebook.net/en_US/fbds.js';\n" +
                "            var s = document.getElementsByTagName('script')[0];\n" +
                "            s.parentNode.insertBefore(fbds, s);\n" +
                "            _fbq.loaded = true;\n" +
                "        }\n" +
                "        _fbq.push([ 'addPixelId', '835299283159573' ]);\n" +
                "    })();\n" +
                "    window._fbq = window._fbq || [];\n" +
                "    window._fbq.push([ 'track', 'PixelInitialized', {} ]);\n" +
                "</script>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <noscript><iframe src=\"https://www.googletagmanager.com/ns.html?id=GTM-PXGN7ZT\" height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe></noscript>\n" +
                "    <noscript>\n" +
                "        <img height=\"1\" width=\"1\" style=\"display:none\" src=\"https://www.facebook.com/tr?id=871037816322217&ev=PageView&noscript=1\" />\n" +
                "    </noscript>\n" +
                "    <noscript>\n" +
                "    <img height=\"1\" width=\"1\" alt=\"\" style=\"display:none\" src=\"https://www.facebook.com/tr?id=835299283159573&amp;ev=PixelInitialized\" />\n" +
                "    </noscript>\n" +
                "    <div id=\"fb-root\"></div>\n" +
                "    <script>\n" +
                "        (function(d, s, id) {\n" +
                "            var js, fjs = d.getElementsByTagName(s)[0];\n" +
                "            if (d.getElementById(id))\n" +
                "                return;\n" +
                "            js = d.createElement(s);\n" +
                "            js.id = id;\n" +
                "            js.src = \"//connect.facebook.net/fi_FI/all.js#xfbml=1\";\n" +
                "            fjs.parentNode.insertBefore(js, fjs);\n" +
                "        }(document, 'script', 'facebook-jssdk'));\n" +
                "    </script>\n" +
                "    <div style=\"display: none; visibility: hidden;\">\n" +
                "        <!--JavaScript Tag // Tag for network 969.1: Sanoma Ad Network // Website: Verkkomediamyynti.fi // Page: Verkkomediamyynti_helsinginhifi_retargeting // Placement: helsinginhifi_retargeting_1x1_btn20 (3452992) // created at: Sep 30, 2011 10:41:19 AM-->\n" +
                "        <script>\n" +
                "        <!--\n" +
                "            if (window.adgroupid == undefined) {\n" +
                "                window.adgroupid = Math.round(Math.random() * 1000);\n" +
                "            }\n" +
                "            document\n" +
                "                    .write('<scr'\n" +
                "                            + 'ipt async language=\"javascript1.1\" src=\"https://adserver.adtech.de/addyn|3.0|969.1|2179776|0|16|ADTECH;cookie=info;alias=helsinginhifi_retargeting_1x1_btn20;loc=100;target=_blank;grp='\n" +
                "                            + window.adgroupid + ';misc='\n" +
                "                            + new Date().getTime() + '\"></scri'+'pt>');\n" +
                "        //-->\n" +
                "        </script>\n" +
                "        <noscript>\n" +
                "            <a href=\"https://adserver.adtech.de/adlink|3.0|969.1|2179776|0|16|ADTECH;loc=300;alias=helsinginhifi_retargeting_1x1_btn20;cookie=info;\" target=\"_blank\"><img\n" +
                "                src=\"https://adserver.adtech.de/adserv|3.0|969.1|2179776|0|16|ADTECH;loc=300;alias=helsinginhifi_retargeting_1x1_btn20;cookie=info;\" border=\"0\"></a>\n" +
                "        </noscript>\n" +
                "        <!-- End of JavaScript Tag -->\n" +
                "    </div>\n" +
                "    <div id=\"header\">\n" +
                "        <div id=\"header-top\" style=\"clear: both;\">\n" +
                "            <div class=\"header-content\">\n" +
                "                <div style=\"float: left;\">\n" +
                "                    <a href=\"/fi/\" class=\"top-navigation-link\">Etusivu</a><a href=\"/fi/sivut/tietoayrityksesta\" class=\"top-navigation-link\">Tietoa yrityksestä</a><a href=\"/fi/contactInfo.do\" class=\"top-navigation-link\">Ota yhteyttä</a><a target=\"_blank\" href=\"https://www.facebook.com/HifiStudioSuomi/\"><img src=\"gfx/common/facebook.png\" alt=\"Facebook\" /></a>&nbsp;&nbsp;\n" +
                "                    <a target=\"_blank\" href=\"https://twitter.com/hifistudio17\"><img src=\"gfx/common/twitter.png\" alt=\"Twitter\" /></a>&nbsp;&nbsp;\n" +
                "                    <a target=\"_blank\" href=\"https://www.youtube.com/user/HifiStudio17/feed\"><img src=\"gfx/common/youtube.png\" alt=\"Youtube\" /></a>&nbsp;&nbsp;\n" +
                "                    <a target=\"_blank\" href=\"https://instagram.com/hifistudio\"><img src=\"gfx/common/instagram.png\" alt=\"Instagram\" /></a>\n" +
                "                    </div>\n" +
                "                <div style=\"float: right;\">\n" +
                "                    <span id=\"login\" data-toggle=\"modal\" data-target=\"#loginModal\"> <span class=\"glyphicon glyphicon-user\"></span>&nbsp;&nbsp;<span class=\"top-navigation-link\"\n" +
                "                            style=\"padding: 0px; cursor: pointer;\"><b>Kirjaudu sisään</b></span></span>&nbsp;&nbsp;&bullet;&nbsp;&nbsp;<span class=\"glyphicon glyphicon-pencil\"></span>&nbsp;&nbsp;<a href=\"/fi/rekisteroidy\" class=\"top-navigation-link\"><b>Rekisteröidy - <i style=\"color: #e40083\">saat lahjaksi</i> AVPlus-lehden kotiin</b></a>&nbsp;\n" +
                "                </div>\n" +
                "                <div class=\"modal fade\" id=\"loginModal\" tabindex=\"-1\" role=\"dialog\" aria-labelledby=\"loginModalLabel\" aria-hidden=\"true\">\n" +
                "                    <div class=\"modal-dialog\">\n" +
                "                        <div class=\"modal-content\">\n" +
                "                            <div class=\"modal-header\">\n" +
                "                                <button type=\"button\" class=\"close\" data-dismiss=\"modal\">\n" +
                "                                    <span aria-hidden=\"true\">&times;</span> <span class=\"sr-only\">Sulje</span>\n" +
                "                                </button>\n" +
                "                                <h4 class=\"modal-title\" id=\"loginModalLabel\">\n" +
                "                                    <img src=\"gfx/common/hifistudio_logo_black.png\" alt=\"HifiStudio logo\" height=\"20\"/> - Sis&auml;&auml;nkirjautuminen</h4>\n" +
                "                            </div>\n" +
                "                            <div class=\"modal-body\">\n" +
                "                                <form name=\"loginForm\" method=\"post\" action=\"/fi/login.do\"><div class=\"form-group\">\n" +
                "                                        <label for=\"username\">K&auml;ytt&auml;j&auml;tunnus tai s&auml;hk&ouml;postiosoite:</label> <input type=\"text\" class=\"form-control\" name=\"username\" id=\"username\"\n" +
                "                                            placeholder=\"K&auml;ytt&auml;j&auml;tunnus tai s&auml;hk&ouml;postiosoite\" />\n" +
                "                                    </div>\n" +
                "                                    <div class=\"form-group\">\n" +
                "                                        <label for=\"password\">Salasana:</label> <input type=\"password\" class=\"form-control\" name=\"password\" id=\"password\"\n" +
                "                                            placeholder=\"Salasana\" />\n" +
                "                                    </div>\n" +
                "                                    <div class=\"form-group\">\n" +
                "                                        <label for=\"keepMeSignedIn\">Pidä minut sisäänkirjautuneena:&nbsp;</label>\n" +
                "                                        <input type=\"checkbox\" checked=\"checked\" type=\"checkbox\" id=\"keepMeSignedIn\" name=\"keepMeSignedIn\" />\n" +
                "                                    </div>\n" +
                "                                    <button type=\"submit\" class=\"btn btn-primary btn-block\">\n" +
                "                                        Kirjaudu</button>\n" +
                "                                </form></div>\n" +
                "                            <div class=\"modal-footer\">\n" +
                "                                <a href=\"/fi/requestNewPassword.do\">Unohtuiko salasana?</a></div>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div id=\"header-main\">\n" +
                "            <div class=\"header-content\">\n" +
                "                <div id=\"logo\">\n" +
                "                    <a href=\"/fi/\"><img src=\"gfx/fi/logo.png\" alt=\"HifiStudio\" /></a></div>\n" +
                "                <div class=\"header-search\">\n" +
                "                    <form action=\"searchProducts.do\" method=\"get\" class=\"form-horizontal\">\n" +
                "                        <div class=\"form-group\">\n" +
                "                            <div class=\"col-sm-10\">\n" +
                "                                <input class=\"form-control\" id=\"productSearchField\" style=\"width: 300px;\" type=\"text\" name=\"productname\" placeholder=\"Etsi tuotteita\"> \n" +
                "                            </div>\n" +
                "                            <div class=\"col-sm-2\" style=\"padding-left: 52px;\">\n" +
                "                                <button type=\"submit\" class=\"btn btn-primary\" id=\"headerSearchButton\" style=\"display: none;\"><span class=\"glyphicon glyphicon-search\"></span></button>\n" +
                "                            </div>\n" +
                "                            <input type=\"hidden\" id=\"function-data\" />\n" +
                "                            <input type=\"hidden\" id=\"data-holder\" />\n" +
                "                        </div>\n" +
                "                    </form>\n" +
                "                </div>\n" +
                "                <a href=\"/fi/checkout.do\" class=\"header-cart\"><div id=\"header-cart\">\n" +
                "                    OSTOSKORI&nbsp;&nbsp;&nbsp;&nbsp;&euro;\n" +
                "                </div></a></div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <div id=\"top-navigation\">\n" +
                "        <div class=\"dropdown\">\n" +
                "            <nav>\n" +
                "                <div class=\"navi-element\">\n" +
                "                    <a href=\"#\" data-toggle=\"dropdown\" id=\"product-line-10\" class=\"dropdown-toggle top-product-line-link\">HIFI</a>\n" +
                "                        <div class=\"dropdown-menu\">\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/akustointi/800\" style=\"color: #000; text-decoration: none;\">AKUSTOINTI</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/akustointi/akustointilevyt/23455\" class=\"top-product-group-link\">Akustointilevyt</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/autolaitteet/170\" style=\"color: #000; text-decoration: none;\">AUTOLAITTEET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/autokaiuttimet/17080\" class=\"top-product-group-link\">Autokaiuttimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/autolaitetarvikkeet/17095\" class=\"top-product-group-link\">Autolaitetarvikkeet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/auton-cd-mp3-soittimet/17015\" class=\"top-product-group-link\">Auton cd/mp3 soittimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/auton-multimediasoittimet/17040\" class=\"top-product-group-link\">Auton multimediasoittimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/autovahvistimet/17060\" class=\"top-product-group-link\">Autovahvistimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/subwoofer-kotelot/17035\" class=\"top-product-group-link\">Subwoofer kotelot</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/subwooferit/17030\" class=\"top-product-group-link\">Subwooferit</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/d-a-muuntimet/197\" style=\"color: #000; text-decoration: none;\">D/A-MUUNTIMET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/d-a-muuntimet/d-a-muuntimet/19700\" class=\"top-product-group-link\">D/a-muuntimet</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/design-hifi/154\" style=\"color: #000; text-decoration: none;\">DESIGN HIFI</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/design-hifi/genevalab/16012\" class=\"top-product-group-link\">Genevalab</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/design-hifi/pro-ject-box-design/15400\" class=\"top-product-group-link\">Pro-ject box design</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/design-hifi/tivoli-audio/16011\" class=\"top-product-group-link\">Tivoli audio</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/laitetelineet/650\" style=\"color: #000; text-decoration: none;\">LAITETELINEET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/laitetelineet/b-o-laitetelineet/60570\" class=\"top-product-group-link\">B&amp;o laitetelineet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/laitetelineet/kaiutinjalustat-telineet/91055\" class=\"top-product-group-link\">Kaiutinjalustat/telineet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/laitetelineet/muut-laitetelineet/91050\" class=\"top-product-group-link\">Muut laitetelineet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/laitetelineet/seinatelineet/91052\" class=\"top-product-group-link\">Seinätelineet</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/levysoittimet/157\" style=\"color: #000; text-decoration: none;\">LEVYSOITTIMET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/levysoittimet/levysoitintarvikkeet/15031\" class=\"top-product-group-link\">Levysoitintarvikkeet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/levysoittimet/levysoittimet/15030\" class=\"top-product-group-link\">Levysoittimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/levysoittimet/riaa-korjaimet/15033\" class=\"top-product-group-link\">Riaa-korjaimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/levysoittimet/aanirasiat/15032\" class=\"top-product-group-link\">Äänirasiat</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/muut-hifilaitteet/190\" style=\"color: #000; text-decoration: none;\">MUUT HIFILAITTEET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/muut-hifilaitteet/b-o-av-laitteet/60520\" class=\"top-product-group-link\">B&amp;o av -laitteet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/muut-hifil";
    }

    private String getTextB()
    {
        return "<!DOCTYPE HTML>\n" +
                "<html xmlns:og=\"http://ogp.me/ns#\" xmlns:fb=\"http://www.facebook.com/2008/fbml\" lang=\"fi\">\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<meta name=\"theme-color\" content=\"#000\">\n" +
                "<meta name=\"google-site-verification\" content=\"-l8cEpWwFrNL61eaBmGNVLxBytB_LBWT6cESeZc4s-E\" />\n" +
                "<meta name=\"google-signin-client_id\" content=\"776208230589-2etb5qlg2ifkfrrqj9nrhje6dckbu4qo.apps.googleusercontent.com\">\n" +
                "\n" +
                "<title>\n" +
                "    SEINÄTELINEET - HifiStudio</title>\n" +
                "\n" +
                "<base href=\"https://www.hifistudio.fi/fi/\">\n" +
                "<meta property=\"og:title\" content=\"HifiStudio - SEINÄTELINEET\" />\n" +
                "<meta property=\"og:type\" content=\"company\" />\n" +
                "\n" +
                "<meta property=\"page_type\" content=\"other\" />\n" +
                "<meta property=\"og:image\" content=\"https://www.hifistudio.fi/fi/gfx/common/hifi_fb_200x200.jpg\" />\n" +
                "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" />\n" +
                "<meta property=\"og:url\" content=\"https://www.hifistudio.fi/fi/tuotteet/laitetelineet/seinatelineet/91052\" />\n" +
                "<meta name=\"Description\" content=\"HifiStudio - tuotemerkkien ja tuotteiden esittely, verkkokauppa sek&auml; yritysinfo ja palvelut.\" />\n" +
                "<meta name=\"Keywords\" content=\"HifiStudio, Hifi, Hifi Studio, kaiuttimet, kaapelit, kuulokkeet, kotiteatteri, vahvistimet, viritinvahvistimet, levysoittimet, design, laitetelineet\" />\n" +
                "\n" +
                "<link rel=\"shortcut icon\" href=\"gfx/common/favicon.ico\" />\n" +
                "<link rel=\"stylesheet\" href=\"css/hifistudio_bootstrap.css\">\n" +
                "<link rel=\"stylesheet\" href=\"css/flexslider.css\">\n" +
                "<link rel=\"stylesheet\" href=\"css/easy-autocomplete.min.css\">\n" +
                "<link rel=\"stylesheet\" href=\"css/hifistudio17.css?version=7.7\">\n" +
                "<script src=\"https://code.jquery.com/jquery-1.12.4.min.js\"></script>\n" +
                "<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>\n" +
                "<script src=\"js/hifistudio17.js?version=7.7\"></script>\n" +
                "<script src=\"js/jquery.easy-autocomplete.min.js\"></script>\n" +
                "<script src=\"js/jquery.flexslider-min.js\"></script>\n" +
                "<script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':\n" +
                "new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],\n" +
                "j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=\n" +
                "'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);\n" +
                "})(window,document,'script','dataLayer','GTM-PXGN7ZT');</script>\n" +
                "\n" +
                "<script>\n" +
                "    var _gaq = _gaq || [];\n" +
                "    _gaq.push([ '_setAccount', 'UA-178451-1' ]);\n" +
                "    _gaq.push([ '_trackPageview' ]);\n" +
                "    (function() {\n" +
                "        var ga = document.createElement('script');\n" +
                "        ga.type = 'text/javascript';\n" +
                "        ga.async = true;\n" +
                "        ga.src = ('https:' == document.location.protocol ? 'https://ssl'\n" +
                "                : 'http://www')\n" +
                "                + '.google-analytics.com/ga.js';\n" +
                "        var s = document.getElementsByTagName('script')[0];\n" +
                "        s.parentNode.insertBefore(ga, s);\n" +
                "    })();\n" +
                "</script>\n" +
                "<script src=\"https://apis.google.com/js/platform.js\" async defer></script>\n" +
                "<script>\n" +
                "(function(w, t, f) {\n" +
                "  var s='script',o='_giosg',h='https://service.giosg.com',e,n;e=t.createElement(s);e.async=1;e.src=h+'/live/';\n" +
                "  w[o]=w[o]||function(){(w[o]._e=w[o]._e||[]).push(arguments)};w[o]._c=f;w[o]._h=h;n=t.getElementsByTagName(s)[0];n.parentNode.insertBefore(e,n);\n" +
                "})(window,document,3057);\n" +
                "</script>\n" +
                "<script>\n" +
                "    !function(f, b, e, v, n, t, s) {\n" +
                "        if (f.fbq)\n" +
                "            return;\n" +
                "        n = f.fbq = function() {\n" +
                "            n.callMethod ? n.callMethod.apply(n, arguments) : n.queue\n" +
                "                    .push(arguments)\n" +
                "        };\n" +
                "        if (!f._fbq)\n" +
                "            f._fbq = n;\n" +
                "        n.push = n;\n" +
                "        n.loaded = !0;\n" +
                "        n.version = '2.0';\n" +
                "        n.queue = [];\n" +
                "        t = b.createElement(e);\n" +
                "        t.async = !0;\n" +
                "        t.src = v;\n" +
                "        s = b.getElementsByTagName(e)[0];\n" +
                "        s.parentNode.insertBefore(t, s)\n" +
                "    }(window, document, 'script', '//connect.facebook.net/en_US/fbevents.js');\n" +
                "\n" +
                "    fbq('init', '871037816322217');\n" +
                "    fbq('track', \"PageView\");\n" +
                "</script>\n" +
                "<script>\n" +
                "!function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?\n" +
                "n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;\n" +
                "n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;\n" +
                "t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,\n" +
                "document,'script','https://connect.facebook.net/en_US/fbevents.js');\n" +
                "fbq('init', '115621915683882'); // Insert your pixel ID here.\n" +
                "fbq('track', 'PageView');\n" +
                "</script>\n" +
                "<noscript><img height=\"1\" width=\"1\" style=\"display:none\"\n" +
                "src=\"https://www.facebook.com/tr?id=115621915683882&ev=PageView&noscript=1\"\n" +
                "/></noscript>\n" +
                "<script>(function() {\n" +
                "        var _fbq = window._fbq || (window._fbq = []);\n" +
                "        if (!_fbq.loaded) {\n" +
                "            var fbds = document.createElement('script');\n" +
                "            fbds.async = true;\n" +
                "            fbds.src = '//connect.facebook.net/en_US/fbds.js';\n" +
                "            var s = document.getElementsByTagName('script')[0];\n" +
                "            s.parentNode.insertBefore(fbds, s);\n" +
                "            _fbq.loaded = true;\n" +
                "        }\n" +
                "        _fbq.push([ 'addPixelId', '835299283159573' ]);\n" +
                "    })();\n" +
                "    window._fbq = window._fbq || [];\n" +
                "    window._fbq.push([ 'track', 'PixelInitialized', {} ]);\n" +
                "</script>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <noscript><iframe src=\"https://www.googletagmanager.com/ns.html?id=GTM-PXGN7ZT\" height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe></noscript>\n" +
                "    <noscript>\n" +
                "        <img height=\"1\" width=\"1\" style=\"display:none\" src=\"https://www.facebook.com/tr?id=871037816322217&ev=PageView&noscript=1\" />\n" +
                "    </noscript>\n" +
                "    <noscript>\n" +
                "    <img height=\"1\" width=\"1\" alt=\"\" style=\"display:none\" src=\"https://www.facebook.com/tr?id=835299283159573&amp;ev=PixelInitialized\" />\n" +
                "    </noscript>\n" +
                "    <div id=\"fb-root\"></div>\n" +
                "    <script>\n" +
                "        (function(d, s, id) {\n" +
                "            var js, fjs = d.getElementsByTagName(s)[0];\n" +
                "            if (d.getElementById(id))\n" +
                "                return;\n" +
                "            js = d.createElement(s);\n" +
                "            js.id = id;\n" +
                "            js.src = \"//connect.facebook.net/fi_FI/all.js#xfbml=1\";\n" +
                "            fjs.parentNode.insertBefore(js, fjs);\n" +
                "        }(document, 'script', 'facebook-jssdk'));\n" +
                "    </script>\n" +
                "    <div style=\"display: none; visibility: hidden;\">\n" +
                "        <!--JavaScript Tag // Tag for network 969.1: Sanoma Ad Network // Website: Verkkomediamyynti.fi // Page: Verkkomediamyynti_helsinginhifi_retargeting // Placement: helsinginhifi_retargeting_1x1_btn20 (3452992) // created at: Sep 30, 2011 10:41:19 AM-->\n" +
                "        <script>\n" +
                "        <!--\n" +
                "            if (window.adgroupid == undefined) {\n" +
                "                window.adgroupid = Math.round(Math.random() * 1000);\n" +
                "            }\n" +
                "            document\n" +
                "                    .write('<scr'\n" +
                "                            + 'ipt async language=\"javascript1.1\" src=\"https://adserver.adtech.de/addyn|3.0|969.1|2179776|0|16|ADTECH;cookie=info;alias=helsinginhifi_retargeting_1x1_btn20;loc=100;target=_blank;grp='\n" +
                "                            + window.adgroupid + ';misc='\n" +
                "                            + new Date().getTime() + '\"></scri'+'pt>');\n" +
                "        //-->\n" +
                "        </script>\n" +
                "        <noscript>\n" +
                "            <a href=\"https://adserver.adtech.de/adlink|3.0|969.1|2179776|0|16|ADTECH;loc=300;alias=helsinginhifi_retargeting_1x1_btn20;cookie=info;\" target=\"_blank\"><img\n" +
                "                src=\"https://adserver.adtech.de/adserv|3.0|969.1|2179776|0|16|ADTECH;loc=300;alias=helsinginhifi_retargeting_1x1_btn20;cookie=info;\" border=\"0\"></a>\n" +
                "        </noscript>\n" +
                "        <!-- End of JavaScript Tag -->\n" +
                "    </div>\n" +
                "    <div id=\"header\">\n" +
                "        <div id=\"header-top\" style=\"clear: both;\">\n" +
                "            <div class=\"header-content\">\n" +
                "                <div style=\"float: left;\">\n" +
                "                    <a href=\"/fi/\" class=\"top-navigation-link\">Etusivu</a><a href=\"/fi/sivut/tietoayrityksesta\" class=\"top-navigation-link\">Tietoa yrityksestä</a><a href=\"/fi/contactInfo.do\" class=\"top-navigation-link\">Ota yhteyttä</a><a target=\"_blank\" href=\"https://www.facebook.com/HifiStudioSuomi/\"><img src=\"gfx/common/facebook.png\" alt=\"Facebook\" /></a>&nbsp;&nbsp;\n" +
                "                    <a target=\"_blank\" href=\"https://twitter.com/hifistudio17\"><img src=\"gfx/common/twitter.png\" alt=\"Twitter\" /></a>&nbsp;&nbsp;\n" +
                "                    <a target=\"_blank\" href=\"https://www.youtube.com/user/HifiStudio17/feed\"><img src=\"gfx/common/youtube.png\" alt=\"Youtube\" /></a>&nbsp;&nbsp;\n" +
                "                    <a target=\"_blank\" href=\"https://instagram.com/hifistudio\"><img src=\"gfx/common/instagram.png\" alt=\"Instagram\" /></a>\n" +
                "                    </div>\n" +
                "                <div style=\"float: right;\">\n" +
                "                    <span id=\"login\" data-toggle=\"modal\" data-target=\"#loginModal\"> <span class=\"glyphicon glyphicon-user\"></span>&nbsp;&nbsp;<span class=\"top-navigation-link\"\n" +
                "                            style=\"padding: 0px; cursor: pointer;\"><b>Kirjaudu sisään</b></span></span>&nbsp;&nbsp;&bullet;&nbsp;&nbsp;<span class=\"glyphicon glyphicon-pencil\"></span>&nbsp;&nbsp;<a href=\"/fi/rekisteroidy\" class=\"top-navigation-link\"><b>Rekisteröidy - <i style=\"color: #e40083\">saat lahjaksi</i> AVPlus-lehden kotiin</b></a>&nbsp;\n" +
                "                </div>\n" +
                "                <div class=\"modal fade\" id=\"loginModal\" tabindex=\"-1\" role=\"dialog\" aria-labelledby=\"loginModalLabel\" aria-hidden=\"true\">\n" +
                "                    <div class=\"modal-dialog\">\n" +
                "                        <div class=\"modal-content\">\n" +
                "                            <div class=\"modal-header\">\n" +
                "                                <button type=\"button\" class=\"close\" data-dismiss=\"modal\">\n" +
                "                                    <span aria-hidden=\"true\">&times;</span> <span class=\"sr-only\">Sulje</span>\n" +
                "                                </button>\n" +
                "                                <h4 class=\"modal-title\" id=\"loginModalLabel\">\n" +
                "                                    <img src=\"gfx/common/hifistudio_logo_black.png\" alt=\"HifiStudio logo\" height=\"20\"/> - Sis&auml;&auml;nkirjautuminen</h4>\n" +
                "                            </div>\n" +
                "                            <div class=\"modal-body\">\n" +
                "                                <form name=\"loginForm\" method=\"post\" action=\"/fi/login.do\"><div class=\"form-group\">\n" +
                "                                        <label for=\"username\">K&auml;ytt&auml;j&auml;tunnus tai s&auml;hk&ouml;postiosoite:</label> <input type=\"text\" class=\"form-control\" name=\"username\" id=\"username\"\n" +
                "                                            placeholder=\"K&auml;ytt&auml;j&auml;tunnus tai s&auml;hk&ouml;postiosoite\" />\n" +
                "                                    </div>\n" +
                "                                    <div class=\"form-group\">\n" +
                "                                        <label for=\"password\">Salasana:</label> <input type=\"password\" class=\"form-control\" name=\"password\" id=\"password\"\n" +
                "                                            placeholder=\"Salasana\" />\n" +
                "                                    </div>\n" +
                "                                    <div class=\"form-group\">\n" +
                "                                        <label for=\"keepMeSignedIn\">Pidä minut sisäänkirjautuneena:&nbsp;</label>\n" +
                "                                        <input type=\"checkbox\" checked=\"checked\" type=\"checkbox\" id=\"keepMeSignedIn\" name=\"keepMeSignedIn\" />\n" +
                "                                    </div>\n" +
                "                                    <button type=\"submit\" class=\"btn btn-primary btn-block\">\n" +
                "                                        Kirjaudu</button>\n" +
                "                                </form></div>\n" +
                "                            <div class=\"modal-footer\">\n" +
                "                                <a href=\"/fi/requestNewPassword.do\">Unohtuiko salasana?</a></div>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div id=\"header-main\">\n" +
                "            <div class=\"header-content\">\n" +
                "                <div id=\"logo\">\n" +
                "                    <a href=\"/fi/\"><img src=\"gfx/fi/logo.png\" alt=\"HifiStudio\" /></a></div>\n" +
                "                <div class=\"header-search\">\n" +
                "                    <form action=\"searchProducts.do\" method=\"get\" class=\"form-horizontal\">\n" +
                "                        <div class=\"form-group\">\n" +
                "                            <div class=\"col-sm-10\">\n" +
                "                                <input class=\"form-control\" id=\"productSearchField\" style=\"width: 300px;\" type=\"text\" name=\"productname\" placeholder=\"Etsi tuotteita\"> \n" +
                "                            </div>\n" +
                "                            <div class=\"col-sm-2\" style=\"padding-left: 52px;\">\n" +
                "                                <button type=\"submit\" class=\"btn btn-primary\" id=\"headerSearchButton\" style=\"display: none;\"><span class=\"glyphicon glyphicon-search\"></span></button>\n" +
                "                            </div>\n" +
                "                            <input type=\"hidden\" id=\"function-data\" />\n" +
                "                            <input type=\"hidden\" id=\"data-holder\" />\n" +
                "                        </div>\n" +
                "                    </form>\n" +
                "                </div>\n" +
                "                <a href=\"/fi/checkout.do\" class=\"header-cart\"><div id=\"header-cart\">\n" +
                "                    OSTOSKORI&nbsp;&nbsp;&nbsp;&nbsp;&euro;\n" +
                "                </div></a></div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <div id=\"top-navigation\">\n" +
                "        <div class=\"dropdown\">\n" +
                "            <nav>\n" +
                "                <div class=\"navi-element\">\n" +
                "                    <a href=\"#\" data-toggle=\"dropdown\" id=\"product-line-10\" class=\"dropdown-toggle top-product-line-link\">HIFI</a>\n" +
                "                        <div class=\"dropdown-menu\">\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/akustointi/800\" style=\"color: #000; text-decoration: none;\">AKUSTOINTI</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/akustointi/akustointilevyt/23455\" class=\"top-product-group-link\">Akustointilevyt</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/autolaitteet/170\" style=\"color: #000; text-decoration: none;\">AUTOLAITTEET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/autokaiuttimet/17080\" class=\"top-product-group-link\">Autokaiuttimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/autolaitetarvikkeet/17095\" class=\"top-product-group-link\">Autolaitetarvikkeet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/auton-cd-mp3-soittimet/17015\" class=\"top-product-group-link\">Auton cd/mp3 soittimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/auton-multimediasoittimet/17040\" class=\"top-product-group-link\">Auton multimediasoittimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/autovahvistimet/17060\" class=\"top-product-group-link\">Autovahvistimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/subwoofer-kotelot/17035\" class=\"top-product-group-link\">Subwoofer kotelot</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/autolaitteet/subwooferit/17030\" class=\"top-product-group-link\">Subwooferit</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/d-a-muuntimet/197\" style=\"color: #000; text-decoration: none;\">D/A-MUUNTIMET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/d-a-muuntimet/d-a-muuntimet/19700\" class=\"top-product-group-link\">D/a-muuntimet</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/design-hifi/154\" style=\"color: #000; text-decoration: none;\">DESIGN HIFI</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/design-hifi/genevalab/16012\" class=\"top-product-group-link\">Genevalab</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/design-hifi/pro-ject-box-design/15400\" class=\"top-product-group-link\">Pro-ject box design</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/design-hifi/tivoli-audio/16011\" class=\"top-product-group-link\">Tivoli audio</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/laitetelineet/650\" style=\"color: #000; text-decoration: none;\">LAITETELINEET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/laitetelineet/b-o-laitetelineet/60570\" class=\"top-product-group-link\">B&amp;o laitetelineet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/laitetelineet/kaiutinjalustat-telineet/91055\" class=\"top-product-group-link\">Kaiutinjalustat/telineet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/laitetelineet/muut-laitetelineet/91050\" class=\"top-product-group-link\">Muut laitetelineet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/laitetelineet/seinatelineet/91052\" class=\"top-product-group-link\">Seinätelineet</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/levysoittimet/157\" style=\"color: #000; text-decoration: none;\">LEVYSOITTIMET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/levysoittimet/levysoitintarvikkeet/15031\" class=\"top-product-group-link\">Levysoitintarvikkeet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/levysoittimet/levysoittimet/15030\" class=\"top-product-group-link\">Levysoittimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/levysoittimet/riaa-korjaimet/15033\" class=\"top-product-group-link\">Riaa-korjaimet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/levysoittimet/aanirasiat/15032\" class=\"top-product-group-link\">Äänirasiat</a></li>\n" +
                "                            </ul>\n" +
                "                            </div>\n" +
                "                        <div class=\"text\" style=\"display: inline-block; vertical-align: top; text-align: left;\">\n" +
                "                            <h4><a href=\"/fi/tuotteet/muut-hifilaitteet/190\" style=\"color: #000; text-decoration: none;\">MUUT HIFILAITTEET</a></h4>\n" +
                "                            <ul class=\"list-unstyled\" style=\"text-align: left;\">\n" +
                "                            <li><a href=\"/fi/tuotteet/muut-hifilaitteet/b-o-av-laitteet/60520\" class=\"top-product-group-link\">B&amp;o av -laitteet</a></li>\n" +
                "                            <li><a href=\"/fi/tuotteet/muut-hifilaitteet/cd-soittimet/15035\" class=\"top-product";
    }
}