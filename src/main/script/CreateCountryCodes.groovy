import com.google.common.collect.Range
import com.neovisionaries.i18n.Region
import com.sun.codemodel.internal.*

import java.time.Year

void createClass(String path) {
    def parser = new XmlSlurper(false, false, true)
    def url = "https://en.wikipedia.org/wiki/ISO_3166-3"
    def html = parser.parse(url)


    JCodeModel model = new JCodeModel()


    model._class("org.meeuw.i18n.FormallyAssignedCode", ClassType.ENUM).with {
        _implements(Region.class)

        JFieldVar name = field(JMod.PRIVATE | JMod.FINAL, String.class, "name")
        JFieldVar locale = field(JMod.PRIVATE | JMod.FINAL, Locale.class, "locale")
        //JFieldVar currency = field(JMod.PRIVATE | JMod.FINAL, Currency.class, "currency")
        JClass stringList = model.ref(List.class).narrow(String.class)

        JFieldVar formerCodes = field(JMod.PRIVATE | JMod.FINAL,stringList, "formerCodes")

        JClass year = model.ref(Year.class)
        JClass range = model.ref(Range.class).narrow(year)

        JFieldVar validity = field(JMod.PRIVATE | JMod.FINAL, range, "validity")


        constructor(JMod.PRIVATE).with {
            body().with {
                assign(JExpr._this().ref(name), param(String.class, "name"))
                assign(JExpr._this().ref(locale), param(Locale.class, "locale"))
                //assign(JExpr._this().ref(currency), param(Currency.class, "currency"))
                assign(JExpr._this().ref(formerCodes), param(stringList, "formerCodes"))
                assign(JExpr._this().ref(validity), param(range, "validity"))
            }
        }



        method(JMod.PUBLIC, String.class, "getName").with {
            annotate(Override.class)
            body()._return(name)
        }
        method(JMod.PUBLIC, Locale.class, "toLocale").with {
            annotate(Override.class)
            body()._return(locale)
        }

        JClass enumClass = model.ref(Region.Type.class)


        method(JMod.PUBLIC, Region.Type.class, "getType").with {
            annotate(Override.class)
            body()._return(enumClass.staticRef(Region.Type.COUNTRY.name()))
        }
        /*method(JMod.PUBLIC, Currency.class, "getCurrency").with {
            annotate(Override.class)
            body()._return(currency)
        }
*/
        method(JMod.PUBLIC, stringList, "getFormerCodes").with {
            body()._return(formerCodes)
        }

        method(JMod.PUBLIC, range, "getValidity").with {
            body()._return(validity)
        }
        println "Slurping " + url + " table "
        int table = 0
        html.depthFirst().each {
            if (it.name() == 'table') {
                table++
            }

            if (table == 1 && it.name() == 'tr') {
                String td0 = it.td[0].text(); // name
                String td0id = it.td[0].'@id'.text()

                if (td0id != null && td0id.length() > 0) {
                    String[] td2 = it.td[2].text().trim().split("[^\\d]", 3) // validity
                    Integer year1= Integer.parseInt(td2[0]);
                    Integer year2= Integer.parseInt(td2[1]);
                    String th0 = it.th[0].span.text() // code
                    String td3 = it.td[3].text()  // new country name

                    JInvocation asList = model.ref(Arrays.class)
                                .staticInvoke("asList")
                    it.td[1].span.each{
                        asList.arg(it.text())
                    } // former codes

                    enumConstant(th0).with {
                        arg(JExpr.lit(td0))
                        //arg(JExpr._null())
                        arg(JExpr._null())
                        arg(asList)

                        arg(model.ref(Range.class).staticInvoke("closed")
                                .arg(year.staticInvoke("of").arg(JExpr.lit(year1)))
                                .arg(year.staticInvoke("of").arg(JExpr.lit(year2)))
                        )
                    }
                }
            }
        }
    }
    File dir = new File(path)
    dir.mkdirs()

    try {
        model.build(dir)
    } catch (Exception e) {
        println e
    }
}
String path = properties['path']
if (path == null) {
    path = "/tmp"
}

createClass(path)
