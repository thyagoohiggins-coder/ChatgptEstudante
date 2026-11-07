package com.estudante.chatgpt;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";
    private static final String[] TAB_NAMES = {"Não entendi", "Organizar", "Exemplos", "Falante nativo"};
    private static final int[] QTY_OPTIONS = {5, 10, 15, 20, 30};

    private EditText frase, palavra, estrutura, definicao;
    private TextView[] outs;
    private TextView[] tabs;
    private LinearLayout[] sections;
    private TextView[] qtyButtons;
    private CheckBox useApp;
    private SharedPreferences prefs;
    private int qty = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        ((TextView) findViewById(R.id.tip)).setText(Html.fromHtml(
                "💡 <b>Dica:</b> para o ChatGPT ficar adequado aos estudos, vá em "
                        + "<b>Configurações → Personalização → Estilo e tom básicos</b> e escolha <b>Eficiente</b>.",
                Html.FROM_HTML_MODE_COMPACT));

        frase = findViewById(R.id.frase);
        palavra = findViewById(R.id.palavra);
        estrutura = findViewById(R.id.estrutura);
        definicao = findViewById(R.id.definicao);
        outs = new TextView[]{findViewById(R.id.out1), findViewById(R.id.out2),
                findViewById(R.id.out3), findViewById(R.id.out4)};
        sections = new LinearLayout[]{findViewById(R.id.sec1), findViewById(R.id.sec2),
                findViewById(R.id.sec3), findViewById(R.id.sec4)};

        useApp = findViewById(R.id.useApp);
        useApp.setChecked(prefs.getBoolean("useApp", false));
        useApp.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("useApp", checked).apply());

        qty = prefs.getInt("qty", 10);
        buildTabs();
        buildQty();

        int[] sendIds = {R.id.send1, R.id.send2, R.id.send3, R.id.send4};
        int[] copyIds = {R.id.copy1, R.id.copy2, R.id.copy3, R.id.copy4};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            findViewById(sendIds[i]).setOnClickListener(v -> send(k));
            findViewById(copyIds[i]).setOnClickListener(v -> {
                if (missingInput(k)) return;
                copy(prompt(k));
                toast("Prompt copiado ✓");
            });
        }

        TextWatcher watcher = new SimpleWatcher(this::render);
        frase.addTextChangedListener(watcher);
        estrutura.addTextChangedListener(watcher);
        definicao.addTextChangedListener(watcher);
        // A palavra digitada na aba 1 já preenche a aba Exemplos
        palavra.addTextChangedListener(new SimpleWatcher(() -> {
            estrutura.setText(palavra.getText());
            render();
        }));

        selectTab(prefs.getInt("tab", 0));
        render();
    }

    // ---------- Prompts ----------

    private String prompt(int k) {
        switch (k) {
            case 0: {
                String f = or(frase, "[frase colada]");
                String p = or(palavra, "[palavra aqui]");
                return "o que significa \"" + p + "\" em \"" + f + "\", estou aprendendo inglês, consumindo conteúdo "
                        + "em inglês falado e escrito e não entendi isso, você pode me dar exemplos de uso para entender "
                        + "melhor ? quanto mais exemplos e mais diversificados melhor responda tudo em inglês para eu "
                        + "consumir mais inglês, use seu conhecimento não precisa puxar da internet, preciso aprender "
                        + "esse conteúdo sem associar/comparar ou traduzir com português, preciso entender o significado "
                        + "em questão, tente não ser longo se não for necessário";
            }
            case 1:
                return "Quero que me faça um favor, você entendeu e identificou a estrutura correta né? agora poderia "
                        + "organizar esse conteúdo para mim da seguinte forma:\n\n"
                        + "os exemplos de uso nas primeiras linhas das suas mensagens, e enumerados\n\n"
                        + "Logo abaixo\n\n"
                        + "nome da estrutura correta + transcrição fonética dela + estrutura gramatical + classe "
                        + "gramatical dela (Tudo na mesma linha)\n\n"
                        + "o significado da estrutura aqui:";
            case 2: {
                String e = or(estrutura, "[palavra/estrutura]");
                String d = or(definicao, "[definição]");
                return "Frases de exemplos de uso com:\n\n"
                        + "A Palavra/Estrutura: " + e + "\n\n"
                        + "definição: " + d + "\n\n"
                        + "Quero " + qty + " exemplos da palavra acima no contexto correto que sugeri.\n"
                        + "Responda em inglês";
            }
            default:
                return "Escolha um assunto aleatório em inglês e vamos falar dele em inglês sempre, você puxa o assunto "
                        + "e eu respondo, quando eu responder, você corrige a frase em inglês na primeira linha "
                        + "destacando onde errei e imediatamente retome ao assunto sem mais delongas.";
        }
    }

    private static String or(EditText e, String fallback) {
        String s = e.getText().toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    private void render() {
        for (int i = 0; i < 4; i++) outs[i].setText(prompt(i));
    }

    private boolean missingInput(int k) {
        if (k == 0 && (frase.getText().toString().trim().isEmpty() || palavra.getText().toString().trim().isEmpty())) {
            toast("Preencha a frase e a palavra");
            return true;
        }
        if (k == 2 && estrutura.getText().toString().trim().isEmpty()) {
            toast("Preencha a palavra/estrutura");
            return true;
        }
        return false;
    }

    // ---------- Envio ----------

    private void send(int k) {
        if (missingInput(k)) return;
        String text = prompt(k);
        copy(text); // cópia de segurança, caso precise colar

        if (useApp.isChecked()) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, text);
            share.setPackage(CHATGPT_PACKAGE);
            try {
                startActivity(share);
                return;
            } catch (ActivityNotFoundException ignored) {
                toast("App do ChatGPT não encontrado, abrindo no navegador");
            }
        }
        // chatgpt.com/?q= abre uma conversa nova e envia o prompt automaticamente
        Uri uri = Uri.parse("https://chatgpt.com/?q=" + Uri.encode(text));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            toast("Nenhum navegador encontrado. O prompt foi copiado.");
        }
    }

    private void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("prompt", text));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ---------- Abas e quantidade ----------

    private void buildTabs() {
        LinearLayout container = findViewById(R.id.tabs);
        tabs = new TextView[TAB_NAMES.length];
        for (int i = 0; i < TAB_NAMES.length; i++) {
            TextView t = new TextView(this);
            t.setText(TAB_NAMES[i]);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            t.setPadding(dp(16), dp(9), dp(16), dp(9));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            t.setLayoutParams(lp);
            final int k = i;
            t.setOnClickListener(v -> selectTab(k));
            container.addView(t);
            tabs[i] = t;
        }
    }

    private void selectTab(int k) {
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == k;
            tabs[i].setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip);
            tabs[i].setTextColor(on ? 0xFFFFFFFF : getColor(R.color.fg));
            sections[i].setVisibility(on ? View.VISIBLE : View.GONE);
        }
        prefs.edit().putInt("tab", k).apply();
    }

    private void buildQty() {
        LinearLayout row = findViewById(R.id.qty);
        qtyButtons = new TextView[QTY_OPTIONS.length];
        for (int i = 0; i < QTY_OPTIONS.length; i++) {
            TextView b = new TextView(this);
            b.setText(String.valueOf(QTY_OPTIONS[i]));
            b.setGravity(Gravity.CENTER);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            if (i > 0) lp.setMarginStart(dp(8));
            b.setLayoutParams(lp);
            final int value = QTY_OPTIONS[i];
            b.setOnClickListener(v -> {
                qty = value;
                prefs.edit().putInt("qty", qty).apply();
                updateQty();
                render();
            });
            row.addView(b);
            qtyButtons[i] = b;
        }
        updateQty();
    }

    private void updateQty() {
        for (int i = 0; i < qtyButtons.length; i++) {
            boolean on = QTY_OPTIONS[i] == qty;
            qtyButtons[i].setBackgroundResource(on ? R.drawable.bg_qty_on : R.drawable.bg_qty);
            qtyButtons[i].setTextColor(on ? 0xFFFFFFFF : getColor(R.color.fg));
            qtyButtons[i].setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static class SimpleWatcher implements TextWatcher {
        private final Runnable onChange;
        SimpleWatcher(Runnable r) { onChange = r; }
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) { onChange.run(); }
    }
}
