package br.com.fiap.agroconnect_api.record;



public record RecommendationCache(RecommendationResponse response, boolean aiGenerated, String modelName, String inputFingerprint) {
}
