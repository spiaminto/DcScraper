package io.github.spiaminto.dcscraper.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScrapeResult {

    private DcBoardsAndComments dcBoardsAndComments;
    private List<ScrapeFailure> failure;
    private LoggingParams loggingParams;

}
