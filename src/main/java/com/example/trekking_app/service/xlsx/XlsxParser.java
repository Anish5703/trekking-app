package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

import static com.example.trekking_app.service.xlsx.XlsxParserColumnDependency.WAYPOINT_NUMBER;

@Service
@Slf4j
@RequiredArgsConstructor
public class XlsxParser {


    private final XlsxParserHelper helper;
    private final String SHEET_NAME = "Trail Map";


   @Transactional
    public ParseOutput parse(@NonNull MultipartFile file ,@NonNull Route route)
    {
        String filename = file.getOriginalFilename();
        List<XlsxParserResult> rawRows = new ArrayList<>();

        if(!Objects.requireNonNull(filename).contains(".xlsx"))
            throw new FileParsingFailedException("invalid poi extraction file ! provide valid xlsx file");

        try(InputStream inputStream = file.getInputStream()) {
            Workbook wb = new XSSFWorkbook(inputStream);
            Sheet sheet = wb.getSheet(SHEET_NAME);
            if (sheet == null)
                throw new IllegalArgumentException(
                        "Sheet '" + SHEET_NAME + "' not found in workbook.");

            Map<String,Integer> indexMap = helper.buildColumnIndex(sheet.getRow(0));
            helper.validateRequiredColumns(indexMap);
            int gpxOrderIndex = 1;
            double prevWpNum = -1;

            Map<String,GpxSegment> gpxSegmentCache = new HashMap<>();
            Map<String, WayPoint> wayPointCache = new HashMap<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++)
            {
                Row row = sheet.getRow(r);
                if (row == null || helper.isBlank(row)) continue;

                // ── GPX segment boundary: waypoint number went backwards ───────
                Double wpNum = helper.dbl(row, indexMap, WAYPOINT_NUMBER);

                if(wpNum!=null && wpNum==0) {
                    gpxOrderIndex++;
                    continue;
                }

                if (wpNum != null && prevWpNum >= 0 && wpNum < prevWpNum) {
                    gpxOrderIndex++;
                }
                if (wpNum != null) prevWpNum = wpNum;
                XlsxParserResult result = helper.parseRow(row, indexMap, route, gpxOrderIndex,
                        gpxSegmentCache,wayPointCache);
                if (result != null) rawRows.add(result);
            }
            return helper.partition(rawRows,route);
        }
        catch (Exception e)
        {
            log.error("xlsx file parsing failed : {}",e.getLocalizedMessage());
            throw new FileParsingFailedException("invalid file failed to parse");
        }
    }


}
