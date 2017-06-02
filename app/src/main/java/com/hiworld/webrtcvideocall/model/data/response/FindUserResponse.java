package com.hiworld.webrtcvideocall.model.data.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by daint on 5/17/2017.
 */

public class FindUserResponse {
    @SerializedName("success")
    private boolean success;
    @SerializedName("data")
    private Data data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        @SerializedName("message")
        private String message;
        @SerializedName("users")
        private List<Users> users;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<Users> getUsers() {
            return users;
        }

        public void setUsers(List<Users> users) {
            this.users = users;
        }

        public static class Users {
            @SerializedName("id")
            private String id;
            @SerializedName("presence")
            private int presence;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public int getPresence() {
                return presence;
            }

            public void setPresence(int presence) {
                this.presence = presence;
            }
        }
    }
}
