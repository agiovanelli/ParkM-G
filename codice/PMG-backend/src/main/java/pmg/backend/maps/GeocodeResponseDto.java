package pmg.backend.maps;

import java.util.List;

public record GeocodeResponseDto(List<GeocodeResultDto> results) {}
