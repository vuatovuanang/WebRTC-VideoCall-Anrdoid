package com.hiworld.webrtcvideocall.model.service;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public interface ApiService {

    class Factory {
        public static ApiService create() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConstant.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            return retrofit.create(ApiService.class);
        }
    }
}
