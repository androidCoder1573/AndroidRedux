package com.cyworks.redux.middleware

/**
 * Desc: 将当前变量转换成另一个变量
 * @author randytu
 */
fun interface Compose<T: Any> {
    /**
     * 对进行参数转换
     * @param next 参数
     * @return 转换结果
     */
    fun compose(next: T): T
}