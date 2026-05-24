package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.dto.poi.XlsxPoiRow;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class XlsxParserService {

    private static final String K_WP_NUMBER     = "waypoint number";
    private static final String K_TRAIL_PATH    = "trail path";
    private static final String K_START_END     = "start or end";
    private static final String K_IMPORTANT     = "important stops";
    private static final String K_NAME_PREFIX = "name of hotel";

    @Transactional
    public List<XlsxPoiRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileParsingFailedException("XLSX file is empty");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        if (!filename.endsWith(".xlsx")) {
            throw new FileParsingFailedException("Only .xlsx files are supported");
        }

        try (InputStream in = file.getInputStream();
             Workbook wb = new XSSFWorkbook(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new FileParsingFailedException("Workbook has no sheets");

            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) throw new FileParsingFailedException("Sheet is empty");

            Row header = it.next();
            Map<String, Integer> idx = indexHeader(header);

            Integer wpCol = idx.get(K_WP_NUMBER);
            if (wpCol == null) {
                throw new FileParsingFailedException(
                        "Required column 'Waypoint Number' not found in header");
            }

            DataFormatter fmt = new DataFormatter();
            List<XlsxPoiRow> rows = new ArrayList<>();

            while (it.hasNext()) {
                Row r = it.next();
                if (r == null) continue;

                Integer wpNumber = readInt(r.getCell(wpCol), fmt);
                if (wpNumber == null) continue; // blank row

                String trailPath  = readStr(r, idx.get(K_TRAIL_PATH), fmt);
                String startEnd   = readStr(r, idx.get(K_START_END), fmt);
                String important  = readStr(r, idx.get(K_IMPORTANT), fmt);
                String name       = readStr(r, idx.get(K_NAME_PREFIX), fmt);

                // Collect everything else (non-empty) into extras for description JSON
                Map<String, String> extras = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> e : idx.entrySet()) {
                    int colIdx = e.getValue();
                    if (colIdx == wpCol) continue;
                    String key = e.getKey();
                    if (key.equals(K_TRAIL_PATH) || key.equals(K_START_END)
                            || key.equals(K_IMPORTANT) || key.equals(K_NAME_PREFIX)) continue;
                    String v = readStr(r, colIdx, fmt);
                    if (v != null && !v.isBlank()) extras.put(originalHeader(header, colIdx, fmt), v);
                }

                rows.add(new XlsxPoiRow(wpNumber, trailPath, startEnd, important, name, extras));
            }
            return rows;

        } catch (IOException e) {
            log.error("Failed to parse xlsx:{}",e.getMessage(),e.getCause());
            throw new FileParsingFailedException("Failed to read XLSX");
        }
    }

    private Map<String, Integer> indexHeader(Row header) {
        Map<String, Integer> idx = new LinkedHashMap<>();
        DataFormatter fmt = new DataFormatter();
        for (Cell c : header) {
            if (c == null) continue;
            String raw = fmt.formatCellValue(c).trim().toLowerCase(Locale.ROOT);
            if (raw.isEmpty()) continue;
            // collapse whitespace
            String key = raw.replaceAll("\\s+", " ");
            // promote prefix match for the long "Name of Hotel ..." column
            if (key.startsWith(K_NAME_PREFIX)) key = K_NAME_PREFIX;
            idx.putIfAbsent(key, c.getColumnIndex());
        }
        return idx;
    }

    private String originalHeader(Row header, int colIdx, DataFormatter fmt) {
        Cell c = header.getCell(colIdx);
        return c == null ? ("col" + colIdx) : fmt.formatCellValue(c).trim();
    }

    private String readStr(Row r, Integer colIdx, DataFormatter fmt) {
        if (colIdx == null) return null;
        Cell c = r.getCell(colIdx);
        if (c == null) return null;
        String v = fmt.formatCellValue(c);
        return v == null ? null : v.trim();
    }

    private Integer readInt(Cell c, DataFormatter fmt) {
        if (c == null) return null;
        try {
            if (c.getCellType() == CellType.NUMERIC) {
                return (int) c.getNumericCellValue();
            }
            String s = fmt.formatCellValue(c).trim();
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }
}
