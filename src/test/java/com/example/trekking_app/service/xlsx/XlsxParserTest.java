package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.example.trekking_app.service.xlsx.XlsxParserColumnDependency.WAYPOINT_NUMBER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XlsxParserTest {

    @Mock
    private XlsxParserHelper helper;

    @InjectMocks
    private XlsxParser xlsxParser;

    private Route route;
    private Map<String, Integer> indexMap;

    @BeforeEach
    void setUp() {
        route = new Route();
        route.setId(1);
        indexMap = Map.of(WAYPOINT_NUMBER, 0);
    }

    private MockMultipartFile buildWorkbookFile(String filename, String sheetName, int dataRowCount) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(WAYPOINT_NUMBER);
            for (int i = 1; i <= dataRowCount; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile("file", filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void testParse_InvalidExtension_ThrowsFileParsingFailedException() {
        MockMultipartFile file = new MockMultipartFile("file", "survey.csv", "text/csv", "data".getBytes());

        assertThrows(FileParsingFailedException.class, () -> xlsxParser.parse(file, route));
        verifyNoInteractions(helper);
    }

    @Test
    void testParse_SheetNotFound_ThrowsFileParsingFailedException() throws IOException {
        MockMultipartFile file = buildWorkbookFile("survey.xlsx", "Wrong Sheet Name", 1);

        assertThrows(FileParsingFailedException.class, () -> xlsxParser.parse(file, route));
    }

    @Test
    void testParse_SkipsBlankRows() throws IOException {
        MockMultipartFile file = buildWorkbookFile("survey.xlsx", "Trail Map", 1);

        when(helper.buildColumnIndex(any(Row.class))).thenReturn(indexMap);
        when(helper.isBlank(any(Row.class))).thenReturn(true);
        when(helper.partition(anyList(), eq(route))).thenReturn(new ParseOutput(List.of(), List.of(), List.of(), List.of(), List.of()));

        xlsxParser.parse(file, route);

        verify(helper, never()).dbl(any(), any(), any());
        verify(helper, never()).parseRow(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void testParse_IncrementsGpxOrderIndex_WhenWaypointNumberGoesBackward() throws IOException {
        MockMultipartFile file = buildWorkbookFile("survey.xlsx", "Trail Map", 2);

        when(helper.buildColumnIndex(any(Row.class))).thenReturn(indexMap);
        when(helper.isBlank(any(Row.class))).thenReturn(false);
        when(helper.dbl(any(), eq(indexMap), eq(WAYPOINT_NUMBER))).thenReturn(3.0, 1.0);
        when(helper.parseRow(any(), any(), any(), anyInt(), any(), any())).thenReturn(null);
        when(helper.partition(anyList(), eq(route))).thenReturn(new ParseOutput(List.of(), List.of(), List.of(), List.of(), List.of()));

        xlsxParser.parse(file, route);

        verify(helper).parseRow(any(), any(), eq(route), eq(1), any(), any());
        verify(helper).parseRow(any(), any(), eq(route), eq(2), any(), any());
    }

    @Test
    void testParse_IncrementsGpxOrderIndex_WhenWaypointNumberIsZero() throws IOException {
        MockMultipartFile file = buildWorkbookFile("survey.xlsx", "Trail Map", 2);

        when(helper.buildColumnIndex(any(Row.class))).thenReturn(indexMap);
        when(helper.isBlank(any(Row.class))).thenReturn(false);
        when(helper.dbl(any(), eq(indexMap), eq(WAYPOINT_NUMBER))).thenReturn(0.0, 1.0);
        when(helper.parseRow(any(), any(), any(), anyInt(), any(), any())).thenReturn(null);
        when(helper.partition(anyList(), eq(route))).thenReturn(new ParseOutput(List.of(), List.of(), List.of(), List.of(), List.of()));

        xlsxParser.parse(file, route);

        verify(helper, never()).parseRow(any(), any(), any(), eq(1), any(), any());
        verify(helper).parseRow(any(), any(), eq(route), eq(2), any(), any());
    }

    @Test
    void testParse_Success_ReturnsHelperPartitionResult() throws IOException {
        MockMultipartFile file = buildWorkbookFile("survey.xlsx", "Trail Map", 1);
        POI poi = new POI();
        WayPoint wp = new WayPoint();
        GpxSegment gpxSegment = new GpxSegment();

        XlsxParserResult parsedRow = XlsxParserResult.ofPOI(wp,poi, 1);
        ParseOutput expected = new ParseOutput(List.of(parsedRow), List.of(wp), List.of(), List.of(), List.of());

        when(helper.buildColumnIndex(any(Row.class))).thenReturn(indexMap);
        when(helper.isBlank(any(Row.class))).thenReturn(false);
        when(helper.dbl(any(), eq(indexMap), eq(WAYPOINT_NUMBER))).thenReturn(1.0);
        when(helper.parseRow(any(), any(), any(), anyInt(), any(), any())).thenReturn(parsedRow);
        when(helper.partition(anyList(), eq(route))).thenReturn(expected);

        ParseOutput result = xlsxParser.parse(file, route);

        assertEquals(expected, result);
        verify(helper).validateRequiredColumns(indexMap);
    }
}