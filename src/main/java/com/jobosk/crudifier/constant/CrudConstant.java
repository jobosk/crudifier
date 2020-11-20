package com.jobosk.crudifier.constant;


public class CrudConstant {

    public abstract class Http {

        public abstract class Param {
            public static final String ID = "id";
        }

        public abstract class Header {
            public static final String TOTAL_COUNT = "x-total-count";
            public static final String EXPOSE_HEADER = "Access-Control-Expose-Headers";
        }
    }
}
