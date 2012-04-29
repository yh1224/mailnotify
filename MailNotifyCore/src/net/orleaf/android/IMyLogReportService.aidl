package net.orleaf.android;

import net.orleaf.android.IMyLogReportListener;

interface IMyLogReportService {
    void addListener(IMyLogReportListener listener);
    void removeListener(IMyLogReportListener listener);
}
