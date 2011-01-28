package org.dbpedia.spotlight.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorCode;

/**
 * This is a customized log4j appender, which will create a new file for every
 * run of the application.
 *
 * @author veera | http://veerasundar.com
 *
 */
//public class NewLogForEachRunFileAppender {

    public class NewLogForEachRunFileAppender extends FileAppender {

        public NewLogForEachRunFileAppender() {
        }

        public NewLogForEachRunFileAppender(Layout layout, String filename,
                                            boolean append, boolean bufferedIO, int bufferSize)
                throws IOException {
            super(layout, filename, append, bufferedIO, bufferSize);
        }

        public NewLogForEachRunFileAppender(Layout layout, String filename,
                                            boolean append) throws IOException {
            super(layout, filename, append);
        }

        public NewLogForEachRunFileAppender(Layout layout, String filename)
                throws IOException {
            super(layout, filename);
        }

        public void activateOptions() {
            if (fileName != null) {
                try {
                    fileName = getNewLogFileName();
                    setFile(fileName, fileAppend, bufferedIO, bufferSize);
                } catch (Exception e) {
                    errorHandler.error("Error while activating log options", e,
                            ErrorCode.FILE_OPEN_FAILURE);
                }
            }
        }

        private String getNewLogFileName() {
            if (fileName != null) {
                final String DOT = ".";
                final String HIPHEN = "-";
                final String UNDERSCORE = "_";
                final File logFile = new File(fileName);
                final String fileName = logFile.getName();
                String newFileName = "";

                final int dotIndex = fileName.indexOf(DOT);
                if (dotIndex != -1) {
                    // the file name has an extension. so, insert the time stamp
                    // between the file name and the extension
                    newFileName = fileName.substring(0, dotIndex) + HIPHEN
                            + +System.currentTimeMillis() + DOT
                            + fileName.substring(dotIndex + 1);
                } else {
                    // the file name has no extension. So, just append the timestamp
                    // at the end.
                    Date date = new Date(System.currentTimeMillis());
                    newFileName = fileName + HIPHEN + date.toString();
                }
                return logFile.getParent() + File.separator + newFileName;
            }
            return null;
        }
    }
//}