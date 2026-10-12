// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;

import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Honest local diagnostics and SHA-256 receipts. No AirDrop engine is shipped. */
public final class MainActivity extends Activity {
    private static final int PICK_FILE = 1, SAVE_REPORT = 2;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Future<?> active;
    private final GenerationGate hashGeneration = new GenerationGate();
    private TextView diagnostic, receiptText;
    private Button hashButton;
    private JSONObject report;
    private FileDigest.Receipt receipt;
    private String pendingExport;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setDecorFitsSystemWindows(false);
        ScrollView scroll = new ScrollView(this);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        scroll.addView(layout);
        TextView title = text(layout, getString(R.string.title));
        title.setTextSize(24);
        text(layout, getString(R.string.intro));
        diagnostic = text(layout, "");
        Button probe = button(layout, getString(R.string.refresh));
        probe.setOnClickListener(v -> refresh());
        Button send = button(layout, getString(R.string.send_unavailable));
        Button receive = button(layout, getString(R.string.receive_unavailable));
        send.setEnabled(false);
        receive.setEnabled(false);
        hashButton = button(layout, getString(R.string.hash));
        hashButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE);
        });
        receiptText = text(layout, getString(R.string.no_receipt));
        Button cancel = button(layout, getString(R.string.cancel));
        cancel.setOnClickListener(v -> {
            if (!hashButton.isEnabled()) {
                hashGeneration.next();
                if (active != null) active.cancel(true);
                receipt = null;
                receiptText.setText(getString(R.string.cancelled));
                hashButton.setEnabled(true);
            }
        });
        Button export = button(layout, getString(R.string.save_report));
        export.setOnClickListener(v -> {
            if (report == null) refresh();
            if (report == null) { receiptText.setText(getString(R.string.export_failed)); return; }
            try {
                JSONObject out = new JSONObject(report.toString());
                if (receipt != null) out.put("localFileReceipt", new JSONObject()
                        .put("bytes", receipt.bytes()).put("sha256", receipt.sha256())
                        .put("transportProven", false));
                pendingExport = out.toString(2);
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.putExtra(Intent.EXTRA_TITLE, "omdrop-capability-report.json");
                startActivityForResult(intent, SAVE_REPORT);
            } catch (JSONException failed) { receiptText.setText(getString(R.string.export_failed)); }
        });
        text(layout, getString(R.string.safety));
        setContentView(scroll);
        if (state != null) pendingExport = state.getString("pendingExport");
        refresh();
    }

    private TextView text(LinearLayout layout, String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(16);
        view.setPadding(0, 10, 0, 10);
        view.setTextIsSelectable(true);
        layout.addView(view);
        return view;
    }
    private Button button(LinearLayout layout, String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setMinimumHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        layout.addView(button);
        return button;
    }

    private void refresh() {
        NativeProbe.Snapshot s = NativeProbe.inspect();
        CapabilityGate.Result gate = CapabilityGate.assess(NativeProbe.facts(s));
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        Network activeNetwork = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities nc = activeNetwork == null ? null : cm.getNetworkCapabilities(activeNetwork);
        boolean wifi = nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean validated = nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        try {
            report = new JSONObject().put("schemaVersion", 1).put("artifact", "capability-gate-only")
                    .put("upstreamSha", "ee6a57d971c207c933a424c0d705044a02985b0a")
                    .put("manufacturer", Build.MANUFACTURER).put("model", Build.MODEL)
                    .put("socManufacturer", Build.SOC_MANUFACTURER).put("socModel", Build.SOC_MODEL)
                    .put("hardware", Build.HARDWARE).put("androidRelease", Build.VERSION.RELEASE)
                    .put("api", Build.VERSION.SDK_INT).put("securityPatch", Build.VERSION.SECURITY_PATCH)
                    .put("nativeLibraryLoaded", s.loaded()).put("capEffReadable", s.capsReadable())
                    .put("capNetAdmin", s.netAdmin()).put("capNetRaw", s.netRaw())
                    .put("emptyPacketSocketOpened", s.packetSocket()).put("packetSocketErrno", s.packetErrno())
                    .put("emptyGenericNetlinkSocketOpened", s.genericNetlinkSocket()).put("netlinkSocketErrno", s.netlinkErrno())
                    .put("selinuxEnforcing", s.selinuxEnforcing()).put("activeNetworkIsWifi", wifi)
                    .put("activeNetworkOsValidated", validated).put("wifiPreservationProven", false)
                    .put("owlBackendImplemented", false).put("airDropImplemented", false)
                    .put("accepted", false).put("blockers", new JSONArray(gate.blockers()));
            diagnostic.setText(getString(R.string.report_display, report.toString(2)));
        } catch (JSONException failed) {
            report = null;
            diagnostic.setText(getString(R.string.diagnostic_failed));
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) {
            if (request == SAVE_REPORT) pendingExport = null;
            return;
        }
        Uri uri = data.getData();
        if (!"content".equals(uri.getScheme())) { receiptText.setText(getString(R.string.document_only)); return; }
        if (request == PICK_FILE) {
            final long generation = hashGeneration.next();
            receipt = null;
            hashButton.setEnabled(false);
            receiptText.setText(getString(R.string.hashing));
            active = worker.submit(() -> {
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    FileDigest.Receipt computed = FileDigest.hash(in, 512L * 1024 * 1024);
                    if (!Thread.currentThread().isInterrupted()) publish(() -> {
                        if (!hashGeneration.owns(generation)) return;
                        receipt = computed;
                        receiptText.setText(getString(R.string.receipt, computed.bytes(), computed.sha256()));
                        hashButton.setEnabled(true);
                    });
                } catch (Exception failed) {
                    if (!Thread.currentThread().isInterrupted()) publish(() -> {
                        if (!hashGeneration.owns(generation)) return;
                        receiptText.setText(getString(R.string.hash_failed));
                        hashButton.setEnabled(true);
                    });
                }
            });
        } else if (request == SAVE_REPORT && pendingExport != null) {
            final String export = pendingExport;
            pendingExport = null;
            worker.submit(() -> {
                try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) throw new java.io.IOException("unavailable output");
                    out.write(export.getBytes(StandardCharsets.UTF_8));
                    publish(() -> receiptText.setText(getString(R.string.saved)));
                } catch (Exception failed) {
                    publish(() -> receiptText.setText(getString(R.string.save_failed)));
                }
            });
        }
    }
    private void publish(Runnable action) {
        runOnUiThread(() -> { if (!isDestroyed() && !isFinishing()) action.run(); });
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("pendingExport", pendingExport);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        hashGeneration.next();
        if (active != null) active.cancel(true);
        worker.shutdownNow();
        super.onDestroy();
    }
}
