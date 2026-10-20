package com.tplatt.shadowshaper;

/**
 * Candid effect implementation state for every catalog record.
 *
 * SCRIPTED means a handler or shared mechanic exists, not that the card has
 * passed in-client comparison. Keep incomplete/unknown data explicit.
 */
public final class ShadowEffectCoverage {
    private ShadowEffectCoverage(){}

    public static String status(String name,String effect,String cost,String power) {
        if(power==null||power.isEmpty())return "DATA BLOCKED: Original card Power not verified.";
        if((cost==null||cost.isEmpty()))return "DATA BLOCKED: Play cost not verified.";
        if(effect==null||effect.trim().isEmpty()||effect.toLowerCase(java.util.Locale.ROOT).contains("no special effects"))
            return "BASIC RULES: No special effect; Power and placement are supported.";
        switch(name){
            case "Xur":
                return "INCOMPLETE: Rock summon needs verified token Power.";
            case "Tomb Ship":
                return "INCOMPLETE: Original random Hive summon pool must be verified.";
            case "Drop Pod":
                return "INCOMPLETE: Original random Cabal Line summon pool must be verified.";
            case "Xuan Wei":
                return "PARTIAL: Damage works; destination-selection movement is pending.";
            default:
                return "SCRIPTED / QA PENDING: Engine references this effect, but full rules accuracy is not certified.";
        }
    }
}
