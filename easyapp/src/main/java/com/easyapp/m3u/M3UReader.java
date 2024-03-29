package com.easyapp.m3u;
import com.easyapp.core.TypeValidator;
import com.easyapp.task.SimpleTask;
import com.http.ceas.entity.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import com.http.ceas.core.HttpClient;
import com.http.ceas.callback.HttpCallback;

public class M3UReader{
    

    public interface Callback{
        void onReadSuccess(List<M3U> listM3U);
        void onReadFailure(Throwable throwable);
    }

    private final Callback callback;

    public M3UReader(Callback callback){
        this.callback = TypeValidator.argumentNonNull(callback, "Callback cannot be null.");
    }


    public void startRead(File file){
        try{
            new ReaderStream().execute(new FileInputStream(file));
        }catch(FileNotFoundException e){
            callback.onReadFailure(e);
        }
    }

    public void startRead(String url){
        HttpClient.with(url).get()
            .then(new HttpCallback(){

                @Override
                public Runnable onResponse(Response response) throws Exception{
                    if(response.isSuccessful())
                        new ReaderStream(response).execute(response.body().toStream());
                    else{
                        onFailure(new UnsupportedOperationException(
                                "unable to continue reading, request response: " 
                                + response.status().message())
                        );
                    }
                    return null;
                }

                @Override
                public void onFailure(Exception p1){
                    callback.onReadFailure(p1);
                }
        });
        /*Client.with(url)
            .get()
            .then(new com.easyapp.net.http.Callback(){
                @Override
                public void onResponse(Response response){
                    if(response.isSuccessful())
                        new ReaderStream(response).execute(response.getBody().stream());
                    else{
                        onFailure(new UnsupportedOperationException(
                                "unable to continue reading, request response: " 
                                + response.getStatus().getMessage())
                        );
                    }
                }
                @Override
                public void onFailure(Throwable throwable){
                    callback.onReadFailure(throwable);
                }
            });*/
    }

    private class ReaderStream extends SimpleTask<InputStream, List<M3U>>{

        private Response response;

        public ReaderStream(Response response){
            this.response = response;
        }

        public ReaderStream(){}

        @Override
        protected List<M3U> doTaskInBackground(InputStream[] params) throws Throwable{
            return M3UParse.createList((params [0]));
        }

        @Override
        protected void onResultTask(List<M3U> result){
            callback.onReadSuccess(result);
        }

        @Override
        protected void onFailureTask(Throwable throwable){
            callback.onReadFailure(throwable);
        }

        @Override
        protected void onFinally(){
            if(response != null) response.disconnect();
        }

    }

}
