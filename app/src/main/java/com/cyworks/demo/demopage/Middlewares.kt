package com.cyworks.demo.demopage

class Middlewares {
//    static Middleware sInfo = getter -> next -> (Dispatch) (action, payload) -> {
    //        final ILogger logger = ReduxManager.getInstance().getLogger();
    //
    //        if (action.equals(reqList)) {
    //            next.dispatch(reqList, null);
    //        } else if (action.equals(changeItem)) {
    //            next.dispatch(changeItem, null);
    //        } else {
    //            next.dispatch(action, payload);
    //        }
    //    };
    //
    //    static Middleware sPerf = getter -> next -> (Dispatch) (action, payload) -> {
    //        final ILogger logger = ReduxManager.getInstance().getLogger();
    //        final long markPrev = System.currentTimeMillis();
    //        next.dispatch(action, payload);
    //        final long markNext = System.currentTimeMillis();
    //    };
    //
    //    /**
    //     * 注入页面Middleware
    //     * @return Middleware list
    //     */
    //    public static List<Middleware> getReducerMiddlewareList() {
    //        List<Middleware> middlewareList = new ArrayList<>();
    //        middlewareList.add(sInfo);
    //        middlewareList.add(sPerf);
    //        return middlewareList;
    //    }
}