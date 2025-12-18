package com.sample;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public class GiftGuideTest {

    public static void main(String[] args) {
        try {
            KieServices ks = KieServices.Factory.get();
            KieContainer kContainer = ks.getKieClasspathContainer();
            KieSession kSession = kContainer.newKieSession("ksession-rules");

            System.out.println("=== Gift Guide Test ===\n");

            kSession.fireAllRules();

            System.out.println("Facts in working memory:");
            for (Object fact : kSession.getObjects()) {
                System.out.println("  " + fact);
            }

            kSession.dispose();
            System.out.println("\nTest OK");

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

