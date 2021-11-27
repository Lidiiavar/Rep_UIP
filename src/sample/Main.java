package sample;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    static int[][] result, A, B;
    static int N, n1, n2;
    int low_limit = 0, upper_limit = 100;
    static boolean isAGenerated = false, isBGenerated = false, isNSet = false, allowUnoptimized = false;
    StringBuilder MessageBuilder;
    TextArea output;
    CheckBox unoptimized;


//This line is changed by me

    @FXML

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();

        Scene rootScene = primaryStage.getScene();

        Button start_action = (Button) rootScene.lookup("#launch");
        Button generate_a = (Button) rootScene.lookup("#generate_a");
        Button generate_b = (Button) rootScene.lookup("#generate_b");

        TextField low_random = (TextField) rootScene.lookup("#lo_limit");
        TextField up_random = (TextField) rootScene.lookup("#up_limit");

        TextField size_prop = (TextField) rootScene.lookup("#size");

        output = (TextArea) rootScene.lookup("#output");
        unoptimized = (CheckBox) rootScene.lookup("#unoptimized");
        MessageBuilder = new StringBuilder();

        unoptimized.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                allowUnoptimized = unoptimized.isSelected();
            }
        });

        start_action.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(isAGenerated && isBGenerated && isNSet) {
                    output.appendText("Task started.\n");
                    action();
                }
                else{
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Error!");
                    alert.setHeaderText("Matrices or N not set");
                    alert.setContentText("Please check if SIZE and RANDOM values set! ");
                    alert.show();
                }
            }
        });

        generate_a.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    N = Integer.parseInt(size_prop.getText());
                    isNSet = true;

                    result = new int[N][N];
                    A = new int[N][N];

                    generate_A();
                    isAGenerated = true;
                    output.appendText("A generated!\n");
                }catch (NumberFormatException e){
                    N = 1000;
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Error!");
                    alert.setHeaderText("Incorrect SIZE input");
                    alert.setContentText("Please check SIZE input. ");
                    alert.show();
                    isNSet = false;
                    isAGenerated = false;
                }
            }
        });

        generate_b.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(isNSet)
                {
                    try{
                        low_limit = Integer.parseInt(low_random.getText());
                        upper_limit = Integer.parseInt(up_random.getText());
                    }catch (NumberFormatException e){
                        low_limit = 0;
                        upper_limit = 100;
                    }
                    B = new int[N][N];
                    generate_B();
                    isBGenerated = true;
                    output.appendText("B generated!\n");
                }
                else{
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Warning!");
                    alert.setHeaderText("Set main matrix first!");
                    alert.show();
                    isBGenerated = false;
                }
            }
        });
    }

    void action(){
        int threads = 10;

        n1 = 0;
        n2 = threads;

        long millis_start = (new Date()).getTime();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < Math.ceil(N / (double)threads); i++) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    calculate_diagonal_multithread(A, B, n1, n2);
                }
            });
            if (i == Math.ceil(N / (double)threads) - 2 && N % threads != 0){
                n1 += threads;
                n2 += N % threads;
            }else{
                n1 += threads;
                n2 += threads;
            }
        }

        executor.shutdown();
        long millis_end = (new Date()).getTime();
        output.appendText("Multithreaded optimized task: " + (millis_end-millis_start) + "ms\n");

        millis_start = (new Date()).getTime();
        calculate_diagonal(A,B);
        millis_end = (new Date()).getTime();

        output.appendText("Singlethread optimized task: " + (millis_end - millis_start) + "ms\n");

        if(allowUnoptimized) {
            millis_start = (new Date()).getTime();
            calculate(A, B);
            millis_end = (new Date()).getTime();


            output.appendText("Singlethread unoptimized task: " + (millis_end - millis_start) + "ms\n");
        }

        millis_start = (new Date()).getTime();
        executor = Executors.newFixedThreadPool(3);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                buildOutputFile(A, "A");
            }
        });
        executor.submit(new Runnable() {
            @Override
            public void run() {
                buildOutputFile(B, "B");
            }
        });
        executor.submit(new Runnable() {
            @Override
            public void run() {
                buildOutputFile(result, "Result");
            }
        });

        executor.shutdown();
        millis_end = (new Date()).getTime();

        output.appendText("File output ended: " + (millis_end - millis_start) + "ms\n");
    }

    public static void main(String[] args) {
        launch(args);
    }

    void generate_B(){
        for (int i = 0; i < N; i++) {
            for (int j = i; j < N; j++) {
                B[i][j] = (int) Math.round(Math.random() * (upper_limit - low_limit) + low_limit);
            }
        }
    }

    void generate_A() {
        for(int i = 0; i < N; i++){
            A[i][i] = (i == 0 || i == N-1) ? N : N-1;
        }
    }

    void calculate(int[][] A, int[][] B){

        for(int i = 0; i < A.length; i++){
            for (int j = 0; j < A[i].length; j++){
                result[i][j] = 0;
                for (int k = 0; k < A[i].length; k++)
                    result[i][j] += A[i][k] * B[k][j];
            }

        }
    }

    public void calculate_diagonal(int[][] A, int[][] B){
        for(int i = 0; i < A.length; i++){
            for(int j = i; j < A[i].length; j++) {
                result[i][j] = A[i][i] * B[i][j];
            }
        }
    }

    public void calculate_diagonal_multithread(int[][] A, int[][] B, int start, int end){
        for(int i = start; i < end; i++){
            for(int j = i; j < A[i].length; j++) {
                result[i][j] = A[i][i] * B[i][j];
            }
        }
    }
    private void buildOutputFile(int[][] matrix, String name){
        String FNAME = name + ".txt";
        StringBuilder data = new StringBuilder();

        for (int i = 0, j = 0; i < matrix.length; j++) {
            if (j == matrix[i].length) {
                j = 0;
                data.append("\n");
                if (++i == matrix.length) break;
            }
            data.append(matrix[i][j] + "\t");
        }

        try {
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(FNAME));
            fileWriter.write(data.toString());
            fileWriter.close();

            data = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

