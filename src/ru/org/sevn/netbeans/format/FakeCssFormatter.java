package ru.org.sevn.netbeans.format;

import java.io.IOException;
import net.revelc.code.formatter.LineEnding;
import net.revelc.code.formatter.css.CssFormatter;

public class FakeCssFormatter extends CssFormatter {
    
    @Override
    public String doFormat(String code, LineEnding ending) throws IOException {
        return super.doFormat(code, ending);
    }
}
