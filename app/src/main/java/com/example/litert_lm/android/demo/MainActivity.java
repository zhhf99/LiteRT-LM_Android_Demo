package com.example.litert_lm.android.demo;

import android.os.Bundle;
import android.util.Log;
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

import com.google.ai.edge.litertlm.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;


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


        byte[] data = new byte[10 * 1024 * 1024];

        try (InputStream is = getApplicationContext().getAssets().open("test.jpg");
             ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ) {
            int n;
            while ((n = is.read(data)) != -1) {
                buffer.write(data, 0, n);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ConversationConfig conversationConfig = new ConversationConfig(
//                Contents.Companion.of("你是一个图像场景分类器，服务于盲人辅助导航系统。你的任务是判断输入图片中的场景属于“路口”还是“人行道”。\n" +
//                        "分类规则：\n" +
//                        "如果图像中存在明显的道路交汇、十字路口、丁字路口、车辆通行交叉区域、红绿灯、斑马线与机动车道交汇等特征，输出：路口\n" +
//                        "如果图像主要是人行道、步行街、人行区域、与机动车道无明显交叉的步行路径，输出：人行道\n" +
//                        "输出要求：\n" +
//                        "只能输出“路口”或“人行道”之一，不能输出任何解释、标点、换行、空格或其他文字，不能输出多余内容，不能提及判断过程，不能输出置信度\n" +
//                        "如果无法确定，优先根据是否存在机动车道交汇判断：有交叉车流风险为路口，无明显交叉车流为人行道")
                Contents.Companion.of("简单描述这张图片的内容")
        );

        send.setOnClickListener(v -> {
            Conversation conversation = engine.createConversation(conversationConfig);

            conversation.sendMessageAsync(Contents.Companion.of(
                    new Content.ImageBytes(data)
            ), new MessageCallback() {
                private StringBuilder builder = new StringBuilder();

                @Override
                public void onMessage(@NonNull Message message) {
                    Log.d(MainActivity.this.toString(), "onMessage...");
                    builder.append(message);
                    runOnUiThread(() -> {
                        output.setText(builder.toString());
                    });
                }

                @Override
                public void onDone() {
                    new Thread(() -> {
                        //have to close conversation in new thread, try closing directly in this method will get stuck
                        //and if try to create new conversation when stuck, app will crash
                        Log.d(MainActivity.this.toString(), "Message Done, cleaning conversation in new thread...");
                        conversation.close();
                        Log.d(MainActivity.this.toString(), "Message Done, conversation closed.");
                    }).start();
                    Log.d(MainActivity.this.toString(), "onDone after clean in new thread.");
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    throwable.printStackTrace();
                }

            }, Collections.emptyMap());

        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        engine.close();
    }
}
