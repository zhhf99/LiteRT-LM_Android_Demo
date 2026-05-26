package com.example.litert_lm.android.demo;

import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.litertlmdemo.LiteLlmWrapper;
import com.google.ai.edge.litertlm.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private TextView output;
    private EditText input;
    private Button send;

    private Engine engine;

    @OptIn(markerClass = ExperimentalApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        output = findViewById(R.id.outputText);
        input = findViewById(R.id.inputEdit);
        send = findViewById(R.id.sendButton);


        File modelFile = ModelUtils.copyModelIfNeeded(
                this,
                "gemma-4-E2B-it.litertlm"
        );

        ExperimentalFlags.INSTANCE.setEnableSpeculativeDecoding(true);
        Engine.Companion.setNativeMinLogSeverity(LogSeverity.ERROR);

        EngineConfig engineConfig = new EngineConfig(
                modelFile.getAbsolutePath(),
                new Backend.GPU(),
                new Backend.GPU(),
                null,
                null,
                null,
                null
        );

        engine = new Engine(engineConfig);
        engine.initialize();

        ConversationConfig conversationConfig = new ConversationConfig(
                Contents.Companion.of("你是一个图像场景分类器，服务于盲人辅助导航系统。你的任务是判断输入图片中的场景属于“路口”还是“人行道”。\n" +
                        "分类规则：\n" +
                        "如果图像中存在明显的道路交汇、十字路口、丁字路口、车辆通行交叉区域、红绿灯、斑马线与机动车道交汇等特征，输出：路口\n" +
                        "如果图像主要是人行道、步行街、人行区域、与机动车道无明显交叉的步行路径，输出：人行道\n" +
                        "输出要求：\n" +
                        "只能输出“路口”或“人行道”之一，不能输出任何解释、标点、换行、空格或其他文字，不能输出多余内容，不能提及判断过程，不能输出置信度\n" +
                        "如果无法确定，优先根据是否存在机动车道交汇判断：有交叉车流风险为路口，无明显交叉车流为人行道")
        );

        send.setOnClickListener(v -> {
            try (Conversation conversation = engine.createConversation(conversationConfig);
                 InputStream is = getApplicationContext().getAssets().open("test.jpg");
            ) {

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                byte[] data = new byte[10 * 1024 * 1024];
                int n;

                while ((n = is.read(data)) != -1) {
                    buffer.write(data, 0, n);
                }

                is.close();

                conversation.sendMessageAsync(Contents.Companion.of(
                        new Content.Text("1+1=几")
                ), new MessageCallback() {
                    @Override
                    public void onMessage(@NonNull Message message) {
                        runOnUiThread(() -> {
                            output.setText(message.toString());
                        });
                    }

                    @Override
                    public void onDone() {

                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {

                    }
                }, Collections.emptyMap());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        engine.close();
    }
}
