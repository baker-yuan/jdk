package cn.baker.test;

public class Main {
    /**
     * -Djdk.proxy.debug=debug -Djdk.proxy.ProxyGenerator.saveGeneratedFiles=true
     */
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        DynamicLogProxy proxy = new DynamicLogProxy(userService);
        UserService proxyUserService = (UserService)proxy.getProxyInstance();
        proxyUserService.test(false);

        // UserService userService = new UserServiceImpl();
        // DynamicLogProxy proxy = new DynamicLogProxy(userService);
        //
        // UserServiceProxy userServiceProxy = new UserServiceProxy(proxy);
        // userServiceProxy.test(false);

    }
}
