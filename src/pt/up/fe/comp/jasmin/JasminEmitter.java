package pt.up.fe.comp.jasmin;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;

public class JasminEmitter implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        String jasminCode = new OllirToJasmin(ollirResult.getOllirClass(),ollirResult.getSymbolTable()).getCode();

        System.out.println("JASMIN CODE:\n" + jasminCode);
        return new JasminResult(ollirResult,jasminCode, Collections.emptyList());
    }
}
