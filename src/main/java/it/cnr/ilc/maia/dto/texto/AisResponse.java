package it.cnr.ilc.maia.dto.texto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class AisResponse {

    private final Integer count;
    private final List<AisData> data;

    public AisResponse(List<Map<String, Object>> response, Integer start, Integer end, AisFilters filters) {
        Map<Integer, AisData> map = new LinkedHashMap<>();
        Number sectionId;
        int id = 0, astart, aend, tstart, tend;
        AisData maia;
        String string;
        for (Map<String, Object> texto : response) {
            sectionId = ((Number) texto.get("section_left_id"));
            sectionId = sectionId == null ? texto.hashCode() : sectionId;
            maia = map.get(sectionId.intValue());
            if (maia == null) {
                maia = new AisData(id++, texto);
                if (filter(maia, filters)) {
                    map.put(sectionId.intValue(), maia);
                } else {
                    maia = null;
                }
            }
            if (maia != null) {
                astart = ((Number) texto.get("start")).intValue() - ((Number) texto.get("left_start")).intValue();
                aend = ((Number) texto.get("end")).intValue() - ((Number) texto.get("left_start")).intValue();
                string = (String) texto.get("extra_value");
                if (string == null || string.isBlank()) {
                    maia.getOffsets().add(new Offset(astart, aend));
                } else {
                    String section = maia.getSection().substring(astart, aend);
                    section = section.replaceAll("[^a-zA-Z0-9'‘’àèéìòù]", " ");
                    tend = 0;
                    for (String token : string.split("\\s+")) {
                        tstart = section.indexOf(token, tend);
                        if (tstart != -1) {
                            tend = tstart + token.length();
                            maia.getOffsets().add(new Offset(astart + tstart, astart + tend));
                        }
                    }
                }
            }
        }
        count = map.size();
        data = map.values().stream().toList().subList(start, Math.min(end, count));
    }

    private static boolean filter(AisData data, AisFilters filters) {
        if (filters.getIndex() != null && !data.getIndex().equals(filters.getIndex())) {
            return false;
        }
        if (filters.getText() != null && !data.getText().toLowerCase().startsWith(filters.getText().toLowerCase())) {
            return false;
        }
        if (filters.getReference() != null && !data.getReference().toLowerCase().startsWith(filters.getReference().toLowerCase())) {
            return false;
        }
        if (filters.getSection() != null && !data.getSection().toLowerCase().contains(filters.getSection().toLowerCase())) {
            return false;
        }
        return true;
    }

}
